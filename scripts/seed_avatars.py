#!/usr/bin/env python3
"""Add or upgrade bundled portraits for the fictional society accounts.

Preview: python3 scripts/seed_avatars.py
Apply:   python3 scripts/seed_avatars.py --apply

The checked-in manifest fixes each person's portrait. Only empty fields or an
explicitly recorded previous sample portrait are replaced. Uploaded photos are
preserved. No account details leave the app.
"""

import argparse
from concurrent.futures import ThreadPoolExecutor, as_completed
import hashlib
import json
import re
import time
from urllib.request import Request, urlopen
import uuid

from seed_society import ROOT, literal, psql
from seed_profile_identities import (
    changes_query as identity_changes_query,
    load_identities,
    preview_sql as identity_preview_sql,
    update_sql as identity_update_sql,
)

MANIFEST = ROOT / 'scripts/demo_avatars.json'
PUBLIC_PREFIX = '/assets/avatars/cedar-court/'
ASSET_DIR = ROOT / 'client/public' / PUBLIC_PREFIX.lstrip('/')
MAX_IMAGE_BYTES = 256 * 1024


def load_portraits():
    portraits = json.loads(MANIFEST.read_text())['portraits']
    for portrait in portraits:
        uuid.UUID(portrait['user_id'])
        assert re.fullmatch(r'[a-z0-9-]+\.jpg', portrait['file']), 'Invalid portrait filename.'
        assert (re.fullmatch(r'https://randomuser\.me/api/portraits/(men|women)/[0-9]{1,2}\.jpg', portrait['source'])
                or re.fullmatch(r'bundled:[a-z0-9_-]+', portrait['source'])), 'Unexpected portrait source.'
        if portrait.get('previous_avatar'):
            assert portrait['previous_avatar'].startswith(PUBLIC_PREFIX), 'Only bundled sample photos may be upgraded.'
    for field in ['user_id', 'username', 'file', 'source']:
        assert len({portrait[field] for portrait in portraits}) == len(portraits), f'Duplicate {field}.'
    return portraits


def valid_jpeg(content):
    return 1000 <= len(content) <= MAX_IMAGE_BYTES and content.startswith(b'\xff\xd8\xff') and content.endswith(b'\xff\xd9')


def prepare_portrait(portrait):
    destination = ASSET_DIR / portrait['file']
    if destination.is_file():
        content = destination.read_bytes()
        assert valid_jpeg(content), f'Invalid local portrait: {destination.name}'
    else:
        assert not portrait['source'].startswith('bundled:'), f'Missing bundled portrait: {destination.name}'
        for attempt in range(3):
            try:
                request = Request(portrait['source'], headers={'User-Agent': 'TenantSphere-Sample-Photos/1.0'})
                with urlopen(request, timeout=20) as response:
                    assert response.headers.get_content_type() == 'image/jpeg', 'Expected a JPEG portrait.'
                    content = response.read(MAX_IMAGE_BYTES + 1)
                assert valid_jpeg(content), f'Invalid downloaded portrait: {destination.name}'
                temporary = destination.with_suffix('.jpg.part')
                temporary.write_bytes(content)
                temporary.replace(destination)
                break
            except Exception:
                if attempt == 2:
                    raise
                time.sleep(attempt + 1)
    digest = hashlib.sha256(content).hexdigest()
    if portrait.get('sha256'):
        assert digest == portrait['sha256'], f'Portrait checksum mismatch: {destination.name}'
    return portrait['file'], digest


def fixture_rows(portraits):
    return ',\n'.join(
        f'({literal(portrait["user_id"])}::uuid, {literal(portrait["username"])}, {literal(PUBLIC_PREFIX + portrait["file"])}, {literal(portrait.get("previous_avatar"))})'
        for portrait in portraits)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apply', action='store_true', help='Prepare portraits and add or upgrade sample photos.')
    parser.add_argument('--container', default='tenant_prod_postgres', help='Local PostgreSQL container.')
    parser.add_argument('--with-identities', action='store_true', help='Update the curated fictional names and hometowns in the same transaction.')
    args = parser.parse_args()
    portraits = load_portraits()
    identities = load_identities() if args.with_identities else []
    username_match = 'u.username = portraits.username'
    if identities:
        by_user = {portrait['user_id']: portrait for portrait in portraits}
        assert all(by_user[entry['user_id']]['username'] == entry['profile']['username'] for entry in identities), 'Identity and portrait manifests must agree.'
        print(psql(args.container, identity_preview_sql(identities)))
        username_match += f' OR u.pkid IN (SELECT user_pkid FROM ({identity_changes_query(identities)}) AS eligible)'
    rows = fixture_rows(portraits)
    summary = json.loads(psql(args.container, f'''WITH portraits (user_id, username, avatar, previous_avatar) AS (VALUES {rows})
        SELECT json_build_object('matched_accounts', count(*),
            'photos_to_update', count(*) FILTER (WHERE p.avatar IS NULL OR btrim(p.avatar) = '' OR p.avatar = portraits.previous_avatar),
            'existing_photos_preserved', count(*) FILTER (WHERE p.avatar IS NOT NULL AND btrim(p.avatar) <> ''
                AND p.avatar IS DISTINCT FROM portraits.previous_avatar))
        FROM profiles p JOIN users u ON u.pkid = p.user_pkid JOIN portraits ON portraits.user_id = u.id
        WHERE NOT u.is_staff AND NOT u.is_superuser AND ({username_match});'''))
    print(json.dumps(summary, indent=2))
    if not args.apply:
        print('Preview only. Pass --apply to prepare the local photos and update the database.')
        return

    ASSET_DIR.mkdir(parents=True, exist_ok=True)
    hashes = {}
    with ThreadPoolExecutor(max_workers=6) as executor:
        futures = [executor.submit(prepare_portrait, portrait) for portrait in portraits]
        for index, future in enumerate(as_completed(futures), 1):
            filename, digest = future.result()
            assert digest not in hashes, f'Duplicate portrait images: {hashes.get(digest)} and {filename}'
            hashes[digest] = filename
            if index % 25 == 0 or index == len(portraits):
                print(f'Local portraits ready: {index}/{len(portraits)}', flush=True)

    result = psql(args.container, f'''BEGIN;
        SET LOCAL lock_timeout = '10s';
        SET LOCAL statement_timeout = '30s';
        SELECT pg_advisory_xact_lock(hashtext('cedar-court-profile-photos'));
        {identity_update_sql(identities) if identities else ''}
        WITH portraits (user_id, username, avatar, previous_avatar) AS (VALUES {rows}), updated AS (
            UPDATE profiles p SET avatar = portraits.avatar, updated_at = now()
            FROM portraits JOIN users u ON u.id = portraits.user_id
            WHERE p.user_pkid = u.pkid AND NOT u.is_staff AND NOT u.is_superuser
                AND u.username = portraits.username
                AND (p.avatar IS NULL OR btrim(p.avatar) = '' OR p.avatar = portraits.previous_avatar)
            RETURNING p.pkid)
        SELECT json_build_object('photos_updated', count(*)) FROM updated;
        COMMIT;''')
    print(result)


if __name__ == '__main__':
    main()
