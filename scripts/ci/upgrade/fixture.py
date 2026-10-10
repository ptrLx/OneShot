#!/usr/bin/env python3
"""Fixture data for the upgrade test, written the way OneShot v1.1.1 stores it, and a canonical dump.

  fixture.py seed-db <diary_entry_db>        insert the rows into a database the old app created
  fixture.py datastore <out-file> <tree-uri> write the DataStore preferences file with the image folder
  fixture.py images <dir>                    write one small image per row, named like the app names them
  fixture.py dump-db <diary_entry_db>        print schema version, identity hash and all rows (read only)
"""
import pathlib
import shutil
import sqlite3
import sys
import tempfile

# Same rows as the Room migration test: date as epoch day, created in epoch seconds, happiness as enum
# name. Every happiness value, epoch day 0, leap days, day 366, a date after 2099, empty strings,
# multi-line, unicode/emoji and SQL-relevant text.
ROWS = [
    (0, 3_600, 1, "OneShot_19700101010000.jpg", "HAPPY", "", ""),
    (18_321, 1_582_970_400, 60, "OneShot_20200229100000.jpg", "NEUTRAL", "Keep going", "Leap day\nsecond line\n"),
    (19_450, 1_680_516_900, 93, "OneShot_20230403101500.jpg", "VERY_HAPPY", "", "A good day"),
    (19_722, 1_704_020_400, 365, "OneShot_20231231110000.jpg", "SAD", "Grüße", "Grüße aus 東京 😀👍🏽"),
    (20_088, 1_735_642_800, 366, "OneShot_20241231120000.jpg", "VERY_SAD", "100%", "'single' \"double\" ; -- %_ \\"),
    (47_541, 4_107_574_800, 60, "OneShot_21000301090000.jpg", "NOT_SPECIFIED", "", "after 2099"),
]
COLUMNS = "date, created, dayOfYear, relativePath, happiness, motivation, textContent"


def seed_db(path):
    con = sqlite3.connect(path)
    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.executemany(f"INSERT INTO DiaryEntry ({COLUMNS}) VALUES (?, ?, ?, ?, ?, ?, ?)", ROWS)
    con.commit()
    con.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    con.close()


def _varint(n):
    out = bytearray()
    while True:
        byte = n & 0x7F
        n >>= 7
        out.append(byte | (0x80 if n else 0))
        if not n:
            return bytes(out)


def _field(number, payload):
    return _varint(number << 3 | 2) + _varint(len(payload)) + payload


def datastore(out, tree_uri):
    # androidx.datastore.preferences PreferenceMap { map<string, Value> preferences = 1 },
    # Value { string string = 5 }: the single key OneShot stores.
    value = _field(5, tree_uri.encode())
    entry = _field(1, b"image_base_location") + _field(2, value)
    pathlib.Path(out).write_bytes(_field(1, entry))


def images(directory):
    # Small camera-like JPEGs (scripts/ci/upgrade/images/1..6.jpg), named the way the app names them.
    out = pathlib.Path(directory)
    out.mkdir(parents=True, exist_ok=True)
    source = pathlib.Path(__file__).with_name("images")
    for i, row in enumerate(ROWS, start=1):
        shutil.copy(source / f"{i}.jpg", out / row[3])


def dump_db(path):
    # Copy database, -wal and -shm so reading never touches the pulled originals.
    src = pathlib.Path(path)
    with tempfile.TemporaryDirectory() as tmp:
        for suffix in ("", "-wal", "-shm"):
            f = src.with_name(src.name + suffix)
            if f.exists():
                shutil.copy(f, pathlib.Path(tmp) / f.name)
        con = sqlite3.connect(pathlib.Path(tmp) / src.name)
        print("user_version", con.execute("PRAGMA user_version").fetchone()[0])
        print("identity_hash", con.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0])
        print("schema", con.execute("SELECT sql FROM sqlite_master WHERE name = 'DiaryEntry'").fetchone()[0])
        for row in con.execute(f"SELECT {COLUMNS} FROM DiaryEntry ORDER BY date"):
            print(repr(row))
        con.close()


def first_image():
    print(ROWS[0][3])


if __name__ == "__main__":
    command, *args = sys.argv[1:]
    {"seed-db": seed_db, "datastore": datastore, "images": images, "dump-db": dump_db, "first-image": first_image}[command](*args)
