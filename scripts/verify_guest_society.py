#!/usr/bin/env python3
"""Verify the local guest experience; --check-voting also exercises and restores one vote."""

import argparse
import hashlib
from http.cookiejar import CookieJar
import json
from pathlib import Path
from urllib.error import HTTPError
from urllib.parse import quote, urlsplit
from urllib.request import HTTPCookieProcessor, Request, build_opener

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check-voting', action='store_true')
    parser.add_argument('--check-avatars', action='store_true', help='Check local portraits and author-photo consistency.')
    args = parser.parse_args()
    jars = {}

    def client():
        jar = CookieJar()
        opener = build_opener(HTTPCookieProcessor(jar))
        jars[opener] = jar
        return opener

    guest = client()
    anonymous = client()
    checks = 0

    def request(path, method='GET', body=None, client=guest):
        nonlocal checks
        parts = urlsplit(path)
        path = parts._replace(path=parts.path.rstrip('/')).geturl()
        headers = {'Content-Type': 'application/json'}
        if method != 'GET':
            # A change carries the CSRF token the server set as a cookie, as the browser app does.
            if csrf(client) is None:
                request('/auth/csrf', client=client)
            headers['X-XSRF-TOKEN'] = csrf(client)
        req = Request('http://localhost:3000/api/v1' + path, method=method,
                      data=json.dumps(body).encode() if body is not None else None,
                      headers=headers)
        try:
            with client.open(req, timeout=30) as response:
                raw = response.read()
                checks += 1
                return response.status, json.loads(raw) if raw else None
        except HTTPError as error:
            return error.code, None

    def csrf(client):
        return next((cookie.value for cookie in jars[client] if cookie.name == 'XSRF-TOKEN'), None)

    def get(path, key):
        status, data = request(path)
        assert status == 200, (path, status)
        return data[key]

    def all_pages(path, key):
        rows, page = [], 1
        while True:
            data = get(path + ('&' if '?' in path else '?') + 'page=' + str(page), key)
            rows.extend(data['results'])
            if not data['next']:
                assert len(rows) == data['count'], (path, len(rows), data['count'])
                assert len({row['id'] for row in rows}) == len(rows), 'Duplicate row across pages'
                return rows
            page += 1

    # Each visit gets a temporary guest account, as "Try as a guest" does.
    assert request('/auth/guest', 'POST', {})[0] == 200
    try:
        tenants = all_pages('/profiles/all', 'profiles')
        technicians = all_pages('/profiles/non-tenant-profiles', 'non_tenant_profiles')
        posts = all_pages('/posts', 'posts')
        photo_records = list(posts)
        assert len(tenants) >= 108 and len(technicians) >= 14 and len(posts) >= 48
        assert all(isinstance(post['is_upvoted'], bool) and isinstance(post['is_downvoted'], bool) for post in posts)
        assert all(not (post['is_upvoted'] and post['is_downvoted']) for post in posts)
        assert [post['created_at'] for post in posts] == sorted((post['created_at'] for post in posts), reverse=True)
        for person in tenants:
            activity = get('/profiles/' + quote(person['slug'], safe='') + '/posts', 'profile_posts')
            assert len(activity['results']) <= 3
            assert all(post['upvotes'] >= 0 and post['downvotes'] >= 0 for post in activity['results'])
        for person in technicians:
            reviews = get('/profiles/' + quote(person['slug'], safe='') + '/reviews', 'profile_reviews')
            photo_records.extend(reviews['reviews']['results'])
            assert reviews['reviews']['count'] >= 6
            assert sum(bucket['count'] for bucket in reviews['distribution']) == reviews['reviews']['count']
            assert 1 <= reviews['average_rating'] <= 5
        bookmarks = all_pages('/posts/bookmarked/posts', 'bookmarked_posts')
        own_posts = all_pages('/posts/my-posts', 'my_posts')
        issues = all_pages('/issues/me', 'my_issues')
        reports = all_pages('/reports/me', 'reports')
        # A new guest has the three newest posts bookmarked, two issues of its own, and nothing written yet.
        assert {post['id'] for post in bookmarks} == {post['id'] for post in posts[:3]}
        assert not own_posts and not reports
        assert sorted(issue['status'] for issue in issues) == ['reported', 'resolved']
        get('/apartments/my-apartment', 'apartment')
        own_profile = get('/profiles/user/my-profile', 'profile')
        top = get('/posts/top-posts', 'top_posts')['results']
        scores = [post['upvotes'] - post['downvotes'] for post in top]
        assert scores == sorted(scores, reverse=True)

        if args.check_avatars:
            manifest = json.loads((ROOT / 'scripts/demo_avatars.json').read_text())
            seeded_users = {portrait['username'] for portrait in manifest['portraits']}
            people = {person['username']: person for person in tenants + technicians}
            photos = {username: person['avatar'] for username, person in people.items()}
            # The retired shared guest account is turned off, so the directories leave it out; its posts keep its photo.
            unlisted = seeded_users - photos.keys()
            assert unlisted <= {'testuser'}, 'A seeded profile is missing from the directories.'
            photos.update({portrait['username']: '/assets/avatars/cedar-court/' + portrait['file']
                           for portrait in manifest['portraits'] if portrait['username'] in unlisted})
            assert all(photos[username] for username in seeded_users), 'A seeded profile has no photo.'
            identities = json.loads((ROOT / 'scripts/demo_profile_identities.json').read_text())['profiles']
            for identity in identities:
                expected = identity['profile']
                person = people[expected['username']]
                for field in ('first_name', 'last_name', 'slug', 'city_of_origin'):
                    assert person[field] == expected[field], 'Inconsistent profile detail: ' + expected['username'] + '/' + field
                assert person['full_name'] == expected['first_name'] + ' ' + expected['last_name']
                assert person['apartment']['unit_number'] == identity['unit_number'], 'A resident changed homes.'
                assert identity['previous']['username'] not in people, 'An old sample identity remains in the directory.'
            assert own_profile['username'] not in people, 'A guest is listed in the directories.'
            assert own_profile['avatar'], 'The guest has no photo.'
            photo_records.extend(top + bookmarks + own_posts)
            for post in posts:
                photo_records.extend(all_pages('/posts/' + post['id'] + '/replies', 'replies'))
            for record in photo_records:
                username = record['author_username']
                if username in seeded_users:
                    assert record['avatar'] == photos[username], 'Inconsistent author photo: ' + username
            paths = {photos[username] for username in seeded_users
                     if photos[username].startswith('/assets/avatars/cedar-court/')}
            if own_profile['avatar'].startswith('/assets/avatars/cedar-court/'):
                paths.add(own_profile['avatar'])
            hashes = set()
            for path in sorted(paths):
                with anonymous.open('http://localhost:3000' + quote(path, safe='/'), timeout=30) as response:
                    assert response.status == 200 and response.headers.get_content_type() == 'image/jpeg', path
                    content = response.read()
                    assert content.startswith(b'\xff\xd8\xff') and content.endswith(b'\xff\xd9'), path
                    digest = hashlib.sha256(content).hexdigest()
                    assert digest not in hashes, 'Duplicate portrait: ' + path
                    hashes.add(digest)
                    checks += 1
            print(f'Verified {len(paths)} locally served portraits across profiles, posts, replies and reviews.')

        if args.check_voting:
            original = next(post for post in reversed(posts) if not post['is_upvoted'] and not post['is_downvoted'])
            path = '/posts/' + original['id'] + '/vote'
            assert request(path, 'PUT', {'vote': 1}, client=anonymous)[0] == 401
            for payload in [{'vote': 2}, {'vote': -2}, {'vote': None}, {}]:
                assert request(path, 'PUT', payload)[0] == 400
            assert request('/posts/00000000-0000-0000-0000-000000000000/vote', 'PUT', {'vote': 1})[0] == 404
            try:
                for vote in [-1, -1, 0, 0, 1, 1, -1, 1, 0]:
                    status, result = request(path, 'PUT', {'vote': vote})
                    assert status == 200
                    assert result['id'] == original['id']
                    assert result['upvotes'] == original['upvotes'] + (vote == 1)
                    assert result['downvotes'] == original['downvotes'] + (vote == -1)
                    assert result['is_upvoted'] == (vote == 1)
                    assert result['is_downvoted'] == (vote == -1)
                    current = next(post for post in all_pages('/posts', 'posts') if post['id'] == original['id'])
                    assert current['updated_at'] == original['updated_at'], 'Voting must not mark a post edited'
                    for field in ['upvotes', 'downvotes', 'is_upvoted', 'is_downvoted']:
                        assert current[field] == result[field]
            finally:
                assert request(path, 'PUT', {'vote': 0})[0] == 200
                current = next(post for post in all_pages('/posts', 'posts') if post['id'] == original['id'])
                for field in ['upvotes', 'downvotes', 'is_upvoted', 'is_downvoted']:
                    assert current[field] == original[field], 'Original vote must be restored'
        print(json.dumps(dict(residents=len(tenants), technicians=len(technicians), posts=len(posts),
                              guest_bookmarks=len(bookmarks), guest_posts=len(own_posts),
                              guest_issues=len(issues), guest_reports=len(reports), checked_requests=checks), indent=2))
        print('Guest profiles, feed, pagination, service reviews and personal activity verified.' +
              (' Voting, validation and authorization verified; original votes restored.' if args.check_voting else ''))
    finally:
        assert request('/auth/logout', 'POST', {})[0] == 204


if __name__ == '__main__':
    main()
