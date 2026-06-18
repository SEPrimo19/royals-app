#!/usr/bin/env python
"""
Build app/src/main/assets/kjv.db from the public-domain aruljohn/Bible-kjv
dataset. KJV is public domain worldwide — license-clean to bundle.

Run once (network required):  python scripts/build_kjv_db.py
Re-runnable: it rebuilds the db from scratch and re-validates.
"""
import json
import os
import sqlite3
import sys
import urllib.request

REPO = "https://api.github.com/repos/aruljohn/Bible-kjv/contents/"
RAW = "https://raw.githubusercontent.com/aruljohn/Bible-kjv/master/"
OUT = os.path.join(os.path.dirname(__file__), "..", "app", "src", "main", "assets", "kjv.db")

# Expected integrity targets for a complete KJV.
EXPECT_BOOKS = 66
EXPECT_VERSES = 31102


def fetch_json(url):
    req = urllib.request.Request(url, headers={"User-Agent": "kjv-builder"})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def main():
    print("Fetching canonical book order…")
    order = fetch_json(RAW + "Books.json")  # 66 names, canonical order
    if len(order) != EXPECT_BOOKS:
        sys.exit(f"Books.json has {len(order)} books, expected {EXPECT_BOOKS}")
    order_index = {name: i + 1 for i, name in enumerate(order)}

    print("Listing repo files…")
    listing = fetch_json(REPO)
    files = {x["name"]: x["download_url"] for x in listing
             if x["name"].endswith(".json") and x["name"] != "Books.json"}

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    if os.path.exists(OUT):
        os.remove(OUT)
    db = sqlite3.connect(OUT)
    db.executescript("""
        CREATE TABLE books (
          book_order    INTEGER PRIMARY KEY,
          name          TEXT NOT NULL,
          testament     TEXT NOT NULL,   -- 'OT' (1-39) or 'NT' (40-66)
          chapter_count INTEGER NOT NULL
        );
        CREATE TABLE verses (
          book_order INTEGER NOT NULL,
          chapter    INTEGER NOT NULL,
          verse      INTEGER NOT NULL,
          text       TEXT NOT NULL
        );
    """)

    total_verses = 0
    seen_books = 0
    for fname, url in files.items():
        data = fetch_json(url)
        name = data["book"]
        if name not in order_index:
            sys.exit(f"Book '{name}' from {fname} not in Books.json order")
        bo = order_index[name]
        testament = "OT" if bo <= 39 else "NT"
        chapters = data["chapters"]
        db.execute(
            "INSERT INTO books(book_order,name,testament,chapter_count) VALUES (?,?,?,?)",
            (bo, name, testament, len(chapters)),
        )
        for ch in chapters:
            cn = int(ch["chapter"])
            for v in ch["verses"]:
                db.execute(
                    "INSERT INTO verses(book_order,chapter,verse,text) VALUES (?,?,?,?)",
                    (bo, cn, int(v["verse"]), v["text"].strip()),
                )
                total_verses += 1
        seen_books += 1

    db.execute("CREATE INDEX idx_verses_loc ON verses(book_order,chapter,verse)")
    db.commit()

    # ---- Integrity gate: refuse to ship an incomplete/garbled Bible ----
    n_books = db.execute("SELECT COUNT(*) FROM books").fetchone()[0]
    n_verses = db.execute("SELECT COUNT(*) FROM verses").fetchone()[0]
    gen11 = db.execute(
        "SELECT text FROM verses WHERE book_order=1 AND chapter=1 AND verse=1"
    ).fetchone()[0]
    db.execute("VACUUM")
    db.close()

    print(f"books={n_books} verses={n_verses}")
    print(f"Gen 1:1 = {gen11!r}")
    problems = []
    if n_books != EXPECT_BOOKS:
        problems.append(f"book count {n_books} != {EXPECT_BOOKS}")
    if n_verses != EXPECT_VERSES:
        problems.append(f"verse count {n_verses} != {EXPECT_VERSES}")
    if not gen11.startswith("In the beginning God created"):
        problems.append("Gen 1:1 text mismatch")
    if problems:
        os.remove(OUT)
        sys.exit("INTEGRITY FAIL: " + "; ".join(problems))

    size_mb = os.path.getsize(OUT) / 1_000_000
    print(f"OK -> {os.path.relpath(OUT)}  ({size_mb:.1f} MB)")


if __name__ == "__main__":
    main()
