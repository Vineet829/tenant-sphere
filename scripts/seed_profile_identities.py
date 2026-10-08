"""Curated identity updates used by seed_avatars.py --with-identities.

Only the recorded fictional sample identities are eligible. Account UUIDs,
credentials, homes and activity are retained; later profile edits are preserved.
"""

import json
import re
import uuid

from seed_society import ROOT, literal

FIELDS = ('username', 'first_name', 'last_name', 'slug', 'country_of_origin', 'city_of_origin')


def load_identities():
    identities = json.loads((ROOT / 'scripts/demo_profile_identities.json').read_text())['profiles']
    for entry in identities:
        uuid.UUID(entry['user_id'])
        assert entry['gender'] in ('female', 'male', 'other')
        assert re.fullmatch(r'[ABC]-[1-6]0[1-8]', entry['unit_number'])
        for version in ('previous', 'profile'):
            profile = entry[version]
            assert set(profile) == set(FIELDS)
            assert all(isinstance(profile[field], str) and profile[field].strip() for field in FIELDS)
            assert re.fullmatch(r'[a-z]+(?:_[a-z]+)+', profile['username'])
            assert profile['slug'] == profile['username'].replace('_', '-')
            assert re.fullmatch(r'[A-Z]{2}', profile['country_of_origin'])
    assert len({entry['user_id'] for entry in identities}) == len(identities)
    for field in ('username', 'slug'):
        assert len({entry['profile'][field] for entry in identities}) == len(identities)
    return identities


def changes_query(identities, lock=False):
    records = literal(json.dumps(identities))
    matches = ' AND '.join(
        f"{('u' if field in ('username', 'first_name', 'last_name') else 'p')}.{field} = change #>> '{{previous,{field}}}'"
        for field in FIELDS)
    return f'''SELECT u.pkid AS user_pkid, p.pkid AS profile_pkid, change -> 'profile' AS profile
        FROM jsonb_array_elements({records}::jsonb) AS change
        JOIN users u ON u.id = (change ->> 'user_id')::uuid
        JOIN profiles p ON p.user_pkid = u.pkid
        JOIN apartments a ON a.tenant_pkid = u.pkid AND a.unit_number = change ->> 'unit_number'
        WHERE u.email LIKE '%@cedarcourt.example' AND NOT u.is_staff AND NOT u.is_superuser
            AND p.occupation = 'tenant' AND p.gender = change ->> 'gender' AND {matches}''' + (
                ' FOR UPDATE OF u, p, a' if lock else '')


def preview_sql(identities):
    return f'''SELECT json_build_object('identities_to_update', count(*),
        'unchanged_or_customized', {len(identities)} - count(*)) FROM ({changes_query(identities)}) AS eligible;'''


def update_sql(identities):
    # The caller owns the transaction. Both tables use the same eligibility
    # snapshot, so a partial match can never rename only half of a profile.
    return f'''CREATE TEMP TABLE cedar_court_identity_changes ON COMMIT DROP AS {changes_query(identities, lock=True)};
        UPDATE users u SET username = c.profile ->> 'username', first_name = c.profile ->> 'first_name',
            last_name = c.profile ->> 'last_name', updated_at = now()
        FROM cedar_court_identity_changes c WHERE u.pkid = c.user_pkid;
        UPDATE profiles p SET slug = c.profile ->> 'slug', country_of_origin = c.profile ->> 'country_of_origin',
            city_of_origin = c.profile ->> 'city_of_origin', updated_at = now()
        FROM cedar_court_identity_changes c WHERE p.pkid = c.profile_pkid;
        SELECT json_build_object('identities_updated', count(*)) FROM cedar_court_identity_changes;'''
