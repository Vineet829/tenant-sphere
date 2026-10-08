#!/usr/bin/env python3
"""Populate the local Spring/PostgreSQL app with fictional Cedar Court activity.

Preview: python3 scripts/seed_society.py
Apply:   python3 scripts/seed_society.py --apply

Uses stable UUIDs, an atomic transaction, and inserts only. Existing accounts,
content edits and votes are preserved. No email or notification is sent.
"""

import argparse
from collections import defaultdict
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import random
import re
import secrets
import shlex
import subprocess
import tempfile
import uuid

from demo_society_data import BIOS, FEMALE_NAMES, MALE_NAMES, POSTS, REPAIRS, REVIEWS, TECHNICIANS

NAMESPACE = uuid.UUID('2d4e9800-1f92-4d41-9e75-07edc952e2ce')
ROOT = Path(__file__).resolve().parents[1]


def identity(key):
    return str(uuid.uuid5(NAMESPACE, key))


def slug(value):
    return re.sub(r'[^a-z0-9]+', '-', value.lower()).strip('-')


class SQL(str):
    pass


def literal(value):
    if isinstance(value, SQL):
        return str(value)
    if value is None:
        return 'NULL'
    if isinstance(value, bool):
        return 'TRUE' if value else 'FALSE'
    if isinstance(value, int):
        return str(value)
    if isinstance(value, datetime):
        value = value.isoformat()
    return "'" + str(value).replace("'", "''") + "'"


def reference(table, external_id):
    assert table in {'users', 'apartments', 'posts', 'tags'}
    return SQL(f'(SELECT pkid FROM {table} WHERE id = {literal(external_id)})')


def psql(container, sql):
    result = subprocess.run(
        ['docker', 'exec', '-i', container, 'sh', '-c',
         'exec psql -XAtq -U "$POSTGRES_USER" -d "$POSTGRES_DB" -v ON_ERROR_STOP=1'],
        input=sql, text=True, capture_output=True, timeout=120)
    if result.returncode:
        # Do not include SQL values or account password hashes in diagnostics.
        errors = [line for line in result.stderr.splitlines() if 'ERROR:' in line]
        raise RuntimeError('\n'.join(errors) or 'PostgreSQL command failed; transaction rolled back.')
    return result.stdout.strip()


def inaccessible_password_hash():
    """Use the running backend's encoder with an unrecorded random password."""
    pid = subprocess.check_output(['lsof', '-nP', '-tiTCP:8000', '-sTCP:LISTEN'], text=True).strip()
    assert pid.isdigit(), 'Start the local TenantSphere backend on port 8000 first.'
    command = subprocess.check_output(['ps', '-ww', '-p', pid, '-o', 'command='], text=True).strip()
    args = shlex.split(command)
    assert 'com.tenantsphere.TenantSphereApplication' in args
    assert str(ROOT / 'target/classes') in command
    classpath_flag = '-cp' if '-cp' in args else '-classpath'
    classpath = args[args.index(classpath_flag) + 1]
    with tempfile.TemporaryDirectory(prefix='tenant-society-password-') as temp:
        program = Path(temp) / 'DemoPassword.java'
        program.write_text('''
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
class DemoPassword {
    public static void main(String[] args) throws Exception {
        String secret = new String(System.in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println(Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8().encode(secret));
    }
}
''')
        result = subprocess.run([args[0], '--class-path', classpath, str(program)],
                                input=secrets.token_urlsafe(48), text=True, capture_output=True, timeout=30)
        assert result.returncode == 0, 'Could not create the sample-account password hash.'
        return next(line for line in result.stdout.splitlines() if line.startswith('$argon2id$'))


class Society:
    def __init__(self, existing, password):
        self.now = datetime.now(timezone.utc).replace(microsecond=0)
        self.password = password
        self.people = {person['username']: person for person in existing['people']}
        self.units = {unit['unit_number']: unit for unit in existing['apartments']}
        self.original_ids = {person['id'] for person in self.people.values()}
        self.new_people = 0
        self.statements = [
            'BEGIN;', "SET LOCAL lock_timeout = '10s';", "SET LOCAL statement_timeout = '90s';",
            "SELECT pg_advisory_xact_lock(hashtext('cedar-court-society-seed'));",
            'CREATE TEMP TABLE new_society_posts (pkid BIGINT PRIMARY KEY) ON COMMIT DROP;',
        ]

    def insert(self, table, values, new_post=None, returning_post=False):
        columns = ', '.join(values)
        data = ', '.join(literal(value) for value in values.values())
        if new_post:
            condition = f' WHERE {reference("posts", new_post)} IN (SELECT pkid FROM new_society_posts)'
            statement = f'INSERT INTO {table} ({columns}) SELECT {data}{condition} ON CONFLICT DO NOTHING'
        else:
            statement = f'INSERT INTO {table} ({columns}) VALUES ({data}) ON CONFLICT DO NOTHING'
        if returning_post:
            statement = f'WITH inserted AS ({statement} RETURNING pkid) INSERT INTO new_society_posts SELECT pkid FROM inserted'
        self.statements.append(statement + ';')

    def person(self, name, gender, occupation, key, bio):
        user_id = identity('user:' + key)
        username = slug(name).replace('-', '_')
        if username in self.people:
            person = self.people[username]
            assert person['id'] == user_id, f'Username collision: {username}; existing account preserved.'
            return person
        rng = random.Random(key)
        joined = self.now - timedelta(days=rng.randint(240, 1100), hours=rng.randrange(24))
        first, last = name.rsplit(' ', 1)
        self.insert('users', dict(id=user_id, username=username, email=username + '@cedarcourt.example',
                                 first_name=first, last_name=last, password=self.password, is_active=True,
                                 date_joined=joined, created_at=joined, updated_at=joined))
        self.insert('profiles', dict(id=identity('profile:' + key), user_pkid=reference('users', user_id),
                                    slug=slug(name), gender=gender, occupation=occupation, bio=bio,
                                    phone_number='', country_of_origin='IN',
                                    city_of_origin=rng.choice(['Pune', 'Pune', 'Mumbai', 'Nashik', 'Bengaluru', 'Hyderabad', 'Jaipur', 'Indore', 'Chennai', 'Kolkata', 'Kochi', 'Nagpur']),
                                    created_at=joined, updated_at=joined))
        person = dict(id=user_id, username=username, occupation=occupation, slug=slug(name), created_at=joined.isoformat())
        self.people[username] = person
        self.new_people += 1
        return person

    def populate_people(self):
        names = [(name.strip(), gender) for text, gender in [(FEMALE_NAMES, 'female'), (MALE_NAMES, 'male')]
                 for name in text.split(';') if name.strip()]
        random.Random(417).shuffle(names)
        index = 0
        self.residents = {}
        by_id = {person['id']: person for person in self.people.values()}
        for block in 'ABC':
            for floor in range(1, 7):
                # Retain the existing C-308 home within that floor's six-home layout.
                numbers = [1, 3, 4, 5, 6, 8] if block == 'C' and floor == 3 else range(1, 7)
                for number in numbers:
                    unit_number = f'{block}-{floor}0{number}'
                    name, gender = names[index]
                    rng = random.Random(unit_number)
                    if unit_number in self.units and self.units[unit_number]['tenant_id']:
                        person = by_id[self.units[unit_number]['tenant_id']]
                    else:
                        bio = '' if index % 5 == 0 else BIOS[index % len(BIOS)]
                        if bio and index >= len(BIOS):
                            bio += rng.choice(['', ' Usually free on weekends.', ' Evenings are best for me.', ' Glad to be part of the building.'])
                        person = self.person(name, gender, 'tenant', 'resident:' + unit_number, bio)
                        unit_id = self.units.get(unit_number, {}).get('id', identity('apartment:' + unit_number))
                        self.insert('apartments', dict(id=unit_id, unit_number=unit_number, building='Cedar Court — ' + block,
                                                      floor=floor, tenant_pkid=reference('users', person['id']),
                                                      created_at=datetime.fromisoformat(person['created_at']),
                                                      updated_at=datetime.fromisoformat(person['created_at'])))
                        self.statements.append(f'UPDATE apartments SET tenant_pkid = {reference("users", person["id"])} WHERE id = {literal(unit_id)} AND tenant_pkid IS NULL;')
                        self.units[unit_number] = dict(id=unit_id, unit_number=unit_number, tenant_id=person['id'], floor=floor)
                    self.residents[unit_number] = person
                    index += 1
        for name, trade, bio in TECHNICIANS:
            self.person(name, 'male', trade, 'technician:' + slug(name), bio)
        self.techs = defaultdict(list)
        for person in sorted(self.people.values(), key=lambda row: row['username']):
            if person['occupation'] in REPAIRS:
                self.techs[person['occupation']].append(person)
        assert len(self.residents) == 108
        assert all(len(self.techs[trade]) >= 2 for trade in REPAIRS)

    def posts(self):
        residents = list(self.residents.values())
        guest = self.people['testuser']
        for index, (unit, age, title, body, tags, replies) in enumerate(POSTS):
            key = slug(title)
            post_id = identity('post:' + key)
            rng = random.Random(key)
            created = self.now - timedelta(days=age, hours=2 + index % 6, minutes=rng.randrange(45))
            author = self.residents[unit]
            self.insert('posts', dict(id=post_id, title=title, slug=key, body=body, author_pkid=reference('users', author['id']),
                                      created_at=created, updated_at=created), returning_post=True)
            for tag in tags:
                tag_slug = slug(tag)
                self.statements.append(f'INSERT INTO tags (name, slug) VALUES ({literal(tag)}, {literal(tag_slug)}) ON CONFLICT DO NOTHING;')
                self.insert('post_tags', dict(post_pkid=reference('posts', post_id), tag_pkid=SQL(f'(SELECT pkid FROM tags WHERE slug = {literal(tag_slug)})')), new_post=post_id)
            reply_authors = set()
            for reply_index, (reply_unit, reply_body) in enumerate(replies):
                replier = self.residents[reply_unit]
                reply_authors.add(replier['id'])
                when = min(self.now - timedelta(minutes=12), created + timedelta(minutes=29 * (reply_index + 1)))
                self.insert('replies', dict(id=identity(f'reply:{key}:{reply_index}'), post_pkid=reference('posts', post_id),
                                            author_pkid=reference('users', replier['id']), body=reply_body, created_at=when, updated_at=when), new_post=post_id)
            eligible = [person for person in residents if person['id'] != author['id'] and person['id'] != guest['id']]
            rng.shuffle(eligible)
            up_count = rng.randint(2, 9) if age <= 2 else rng.randint(5, 29)
            down_count = rng.randint(1, 4) if 'Parking' in tags or 'Pets' in tags else rng.choices([0, 1, 2], [8, 2, 1])[0]
            voters = eligible[:up_count + down_count]
            for position, voter in enumerate(voters):
                table = 'post_upvotes' if position < up_count else 'post_downvotes'
                self.insert(table, dict(post_pkid=reference('posts', post_id), user_pkid=reference('users', voter['id'])), new_post=post_id)
            saved_by = rng.sample(eligible, rng.randint(2, 9))
            if index in {3, 4, 6, 7, 11, 15, 16, 19, 25, 31}:
                saved_by.append(guest)
            for person in saved_by:
                self.insert('post_bookmarks', dict(post_pkid=reference('posts', post_id), user_pkid=reference('users', person['id'])), new_post=post_id)
            viewed_by = set(reply_authors) | {author['id']} | {voter['id'] for voter in voters}
            viewed_by |= {person['id'] for person in rng.sample(residents, rng.randint(12, 78))}
            for person_id in sorted(viewed_by):
                when = created + (self.now - created) * rng.uniform(.15, .95)
                self.insert('content_views', dict(id=identity(f'view:{key}:{person_id}'), content_type='post',
                                                  object_pkid=reference('posts', post_id), user_pkid=reference('users', person_id),
                                                  last_viewed=when, created_at=when, updated_at=when), new_post=post_id)
        self.statements.append('''UPDATE posts p SET
            upvotes = (SELECT count(*) FROM post_upvotes v WHERE v.post_pkid = p.pkid),
            downvotes = (SELECT count(*) FROM post_downvotes v WHERE v.post_pkid = p.pkid)
            WHERE p.pkid IN (SELECT pkid FROM new_society_posts);''')

    def issue(self, key, unit, technician, trade, index, age, status):
        created = self.now - timedelta(days=age, hours=(index * 7) % 24)
        resolved = created + timedelta(days=1 + index % 4) if status == 'resolved' else None
        updated = resolved or (created + timedelta(hours=5) if status == 'in_progress' else created)
        title, description = REPAIRS[trade][index % len(REPAIRS[trade])]
        self.insert('issues', dict(id=identity('issue:' + key), apartment_pkid=reference('apartments', self.units[unit]['id']),
                                  reported_by_pkid=reference('users', self.residents[unit]['id']),
                                  assigned_to_pkid=reference('users', technician['id']) if status != 'reported' else None,
                                  title=title, description=description, status=status,
                                  priority='high' if trade == 'electrician' and index % 3 == 2 else ('medium' if index % 3 else 'low'),
                                  resolved_on=resolved.date().isoformat() if resolved else None, created_at=created, updated_at=updated))
        return resolved

    def maintenance_and_reviews(self):
        sample_units = [unit for unit, person in self.residents.items() if person['id'] == identity('user:resident:' + unit)]
        for trade, technicians in self.techs.items():
            for technician in technicians:
                rng = random.Random('work:' + technician['username'])
                choices = [unit for unit in sample_units if trade != 'roofer' or unit[2] == '6']
                for index, unit in enumerate(rng.sample(choices, 6)):
                    key = technician['username'] + ':' + str(index)
                    resolved = self.issue(key, unit, technician, trade, index, rng.randint(10, 135), 'resolved')
                    rating = rng.choices([2, 3, 4, 5], [1, 2, 7, 10])[0]
                    # Lower scores mention the actual source of dissatisfaction.
                    comment = ("The repair is holding, but two appointments were missed before the visit." if rating == 2 else
                               "The job was completed, though getting a confirmed time took longer than I expected." if rating == 3 else
                               REVIEWS[trade][(index + rng.randrange(7)) % len(REVIEWS[trade])])
                    when = resolved + timedelta(hours=rng.randint(18, 72))
                    self.insert('ratings', dict(id=identity('rating:' + key), rated_user_pkid=reference('users', technician['id']),
                                                rating_user_pkid=reference('users', self.residents[unit]['id']), rating=rating, comment=comment,
                                                created_at=when, updated_at=when))
        rng = random.Random('current-maintenance')
        for index, unit in enumerate(rng.sample(sample_units, 18)):
            trade = list(REPAIRS)[index % len(REPAIRS)]
            technician = self.techs[trade][index % len(self.techs[trade])]
            self.issue('open:' + unit, unit, technician, trade, index, 1 + index % 6, 'reported' if index % 3 == 0 else 'in_progress')
        guest_unit = next(unit for unit, person in self.residents.items() if person['username'] == 'testuser')
        for index, (trade, age, status) in enumerate([
            ('plumber', 120, 'resolved'), ('electrician', 90, 'resolved'), ('carpenter', 60, 'resolved'),
            ('hvac', 35, 'resolved'), ('painter', 18, 'resolved'), ('plumber', 2, 'in_progress'),
            ('electrician', 1, 'reported'), ('roofer', 4, 'in_progress'),
        ]):
            self.issue('guest:' + str(index), guest_unit, self.techs[trade][index % len(self.techs[trade])], trade, index, age, status)

    def reports(self):
        concerns = [
            ('Bicycles blocking the stair landing', 'Two bicycles have been parked on the stair landing for several evenings. Could they be moved to the cycle stand so people can pass easily?'),
            ('Visitor car left across the marked bay', 'The same car has been left across two visitor bays on three evenings this week. A reminder about the parking lines would help.'),
            ('Drilling after quiet hours', 'Drilling continued after ten on two weeknights. Please ask that the remaining work be done during the daytime slot.'),
            ('Boxes outside the lift', 'Packing boxes have been left beside the lift for a few days. They narrow the entrance when someone is carrying shopping.'),
            ('Water dripping onto the balcony below', 'Water from washing the balcony has been coming onto the clothes drying below. Could the cleaning time be coordinated with the downstairs flat?'),
            ('Footwear rack narrowing the corridor', 'The new rack projects well into the corridor near the stairs. There should still be room for people to pass with bags.'),
            ('Waste bag left beside the collection bucket', 'Loose waste was left beside the bucket twice this week and was scattered by the time the team arrived. Please use a closed container.'),
            ('Loud gathering late on Sunday', 'Music and moving chairs could be heard after eleven on Sunday. A reminder about the quiet-hour timing would be appreciated.'),
            ('Pet waste on the rear path', 'There has been pet waste on the rear walking path on two mornings. Please remind the household to carry a bag on walks.'),
        ]
        targets = [unit for unit, person in self.residents.items() if person['id'] == identity('user:resident:' + unit)]
        for index, (title, description) in enumerate(concerns):
            target = self.residents[targets[index * 7]]
            reporter = self.people['testuser'] if index < 2 else self.residents[targets[index * 7 + 3]]
            created = self.now - timedelta(days=3 + index * 9, hours=index)
            self.insert('reports', dict(id=identity('report:' + str(index)), title=title, slug=slug(title),
                                       reported_by_pkid=reference('users', reporter['id']), reported_user_pkid=reference('users', target['id']),
                                       description=description, created_at=created, updated_at=created))
            self.statements.append(f'''UPDATE profiles SET
                report_count = (SELECT count(DISTINCT reported_by_pkid) FROM reports WHERE reported_user_pkid = profiles.user_pkid AND reported_by_pkid <> reported_user_pkid),
                reputation = GREATEST(0, 100 - 20 * (SELECT count(DISTINCT reported_by_pkid) FROM reports WHERE reported_user_pkid = profiles.user_pkid AND reported_by_pkid <> reported_user_pkid))
                WHERE user_pkid = {reference('users', target['id'])};''')

    def build(self):
        self.populate_people()
        self.posts()
        self.maintenance_and_reviews()
        self.reports()
        self.statements.append('COMMIT;')
        return '\n'.join(self.statements)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='Write the fixtures to the local database.')
    parser.add_argument('--container', default='tenant_prod_postgres', help='Local PostgreSQL container.')
    args = parser.parse_args()
    existing = json.loads(psql(args.container, '''SELECT json_build_object(
        'people', (SELECT COALESCE(json_agg(json_build_object('id', u.id, 'username', u.username,
            'occupation', p.occupation, 'slug', p.slug, 'created_at', u.created_at)), '[]')
            FROM users u JOIN profiles p ON p.user_pkid = u.pkid WHERE NOT u.is_staff AND NOT u.is_superuser),
        'apartments', (SELECT COALESCE(json_agg(json_build_object('id', a.id, 'unit_number', a.unit_number,
            'floor', a.floor, 'tenant_id', u.id)), '[]') FROM apartments a LEFT JOIN users u ON u.pkid = a.tenant_pkid)
        );'''))
    assert any(person['username'] == 'testuser' for person in existing['people']), 'Expected the existing guest account.'
    seed = Society(existing, inaccessible_password_hash() if args.apply else '!preview-only')
    sql = seed.build()
    print(json.dumps(dict(new_people=seed.new_people, occupied_homes=len(seed.residents),
                          technicians=sum(map(len, seed.techs.values())), community_posts=len(POSTS)), indent=2))
    if not args.apply:
        print('Preview only. Pass --apply to populate the local database.')
        return
    psql(args.container, sql)
    counts = psql(args.container, '''SELECT json_build_object(
        'residents', (SELECT count(*) FROM profiles p JOIN users u ON u.pkid = p.user_pkid WHERE p.occupation = 'tenant' AND NOT u.is_staff AND NOT u.is_superuser),
        'technicians', (SELECT count(*) FROM profiles WHERE occupation <> 'tenant'),
        'occupied_homes', (SELECT count(*) FROM apartments WHERE tenant_pkid IS NOT NULL),
        'posts', (SELECT count(*) FROM posts), 'replies', (SELECT count(*) FROM replies),
        'issues', (SELECT count(*) FROM issues), 'reviews', (SELECT count(*) FROM ratings),
        'reports', (SELECT count(*) FROM reports));''')
    print('Society populated:', counts)


if __name__ == '__main__':
    main()
