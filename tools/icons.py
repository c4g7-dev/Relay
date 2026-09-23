#!/usr/bin/env python3
"""Draws Relay's GUI icons.

Every icon is a white, round-capped line drawing on a transparent 128x128
canvas. They are rendered at 4x and downsampled so the in-game blit (usually
10-12 px) stays smooth. Run from the repository root:

    python3 tools/icons.py
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 128
SCALE = 4
STROKE = 10
OUT = Path("src/main/resources/assets/relaychat/textures/gui/icons")
WHITE = (255, 255, 255, 255)


class Pen:
    def __init__(self):
        self.image = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
        self.draw = ImageDraw.Draw(self.image)
        self.width = STROKE * SCALE

    def _p(self, x, y):
        return (x * SCALE, y * SCALE)

    def dot(self, x, y, r=None):
        r = (r if r is not None else STROKE / 2) * SCALE
        cx, cy = self._p(x, y)
        self.draw.ellipse((cx - r, cy - r, cx + r, cy + r), fill=WHITE)

    def line(self, *points):
        pts = [self._p(x, y) for x, y in points]
        self.draw.line(pts, fill=WHITE, width=self.width, joint="curve")
        for x, y in points:
            self.dot(x, y)

    def rect(self, x0, y0, x1, y1, radius=8):
        self.draw.rounded_rectangle(
            (x0 * SCALE, y0 * SCALE, x1 * SCALE, y1 * SCALE),
            radius=radius * SCALE, outline=WHITE, width=self.width)

    def fill_rect(self, x0, y0, x1, y1, radius=0):
        self.draw.rounded_rectangle(
            (x0 * SCALE, y0 * SCALE, x1 * SCALE, y1 * SCALE),
            radius=radius * SCALE, fill=WHITE)

    def circle(self, cx, cy, r):
        self.draw.ellipse(((cx - r) * SCALE, (cy - r) * SCALE, (cx + r) * SCALE, (cy + r) * SCALE),
                          outline=WHITE, width=self.width)

    def arc(self, cx, cy, r, start, end):
        self.draw.arc(((cx - r) * SCALE, (cy - r) * SCALE, (cx + r) * SCALE, (cy + r) * SCALE),
                      start, end, fill=WHITE, width=self.width)
        for angle in (start, end):
            a = math.radians(angle)
            self.dot(cx + r * math.cos(a), cy + r * math.sin(a))

    def save(self, name):
        OUT.mkdir(parents=True, exist_ok=True)
        self.image.resize((SIZE, SIZE), Image.LANCZOS).save(OUT / f"{name}.png")
        (OUT / f"{name}.png.mcmeta").write_text('{\n  "texture": {\n    "blur": true,\n    "clamp": true\n  }\n}\n')


def icon(fn):
    pen = Pen()
    fn(pen)
    pen.save(fn.__name__)
    return fn


@icon
def menu(p):
    for y in (34, 64, 94):
        p.line((22, y), (106, y))


@icon
def plus(p):
    p.line((64, 22), (64, 106))
    p.line((22, 64), (106, 64))


@icon
def cross(p):
    p.line((30, 30), (98, 98))
    p.line((98, 30), (30, 98))


@icon
def back(p):
    p.line((76, 28), (42, 64), (76, 100))


@icon
def forward(p):
    p.line((52, 28), (86, 64), (52, 100))


@icon
def check(p):
    p.line((24, 66), (52, 94), (104, 36))


@icon
def bin(p):
    p.line((20, 34), (108, 34))
    p.line((48, 34), (52, 18), (76, 18), (80, 34))
    p.rect(30, 34, 98, 112, radius=10)
    p.line((54, 56), (54, 90))
    p.line((74, 56), (74, 90))


@icon
def gear(p):
    cx, cy = 64, 64
    teeth = 8
    outer, inner = 50, 38
    points = []
    for i in range(teeth * 4):
        angle = math.radians(i * 360 / (teeth * 4) - 90 + 360 / (teeth * 8))
        r = outer if (i % 4) in (0, 1) else inner
        points.append((cx + r * math.cos(angle), cy + r * math.sin(angle)))
    points.append(points[0])
    p.line(*points)
    p.circle(cx, cy, 15)


@icon
def lock(p):
    p.rect(24, 56, 104, 112, radius=12)
    p.arc(64, 54, 24, 180, 360)
    p.line((40, 54), (40, 56))
    p.line((88, 54), (88, 56))
    p.dot(64, 84, 7)


@icon
def unlock(p):
    p.rect(24, 56, 104, 112, radius=12)
    p.arc(64, 42, 24, 180, 350)
    p.line((40, 42), (40, 56))
    p.dot(64, 84, 7)


@icon
def regex(p):
    # A dot and an asterisk: the ".*" everyone types first.
    p.dot(30, 96, 9)
    p.line((82, 26), (82, 90))
    for angle in (30, 150):
        a = math.radians(angle)
        dx, dy = 32 * math.cos(a), 32 * math.sin(a)
        p.line((82 - dx, 58 - dy), (82 + dx, 58 + dy))


@icon
def window(p):
    p.rect(16, 22, 112, 106, radius=8)
    p.line((16, 46), (112, 46))
    p.dot(30, 34, 4)
    p.dot(42, 34, 4)


@icon
def tab(p):
    p.rect(16, 22, 112, 106, radius=8)
    p.line((16, 46), (112, 46))
    p.line((56, 22), (56, 46))


@icon
def server(p):
    p.rect(20, 18, 108, 58, radius=8)
    p.rect(20, 70, 108, 110, radius=8)
    p.dot(38, 38, 6)
    p.dot(38, 90, 6)


def mod_icon():
    """The 128 px mod-list icon: an accent chat bubble carrying a relay (swap) arrow pair."""
    big = 128 * SCALE
    image = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    s = SCALE
    draw.rounded_rectangle((4 * s, 4 * s, 124 * s, 124 * s), radius=26 * s, fill=(16, 16, 22, 255))
    accent = (90, 140, 255, 255)
    draw.rounded_rectangle((20 * s, 26 * s, 108 * s, 88 * s), radius=16 * s, fill=accent)
    draw.polygon([(34 * s, 84 * s), (34 * s, 106 * s), (58 * s, 84 * s)], fill=accent)
    white = (255, 255, 255, 255)
    w = 7 * s
    # upper arrow → , lower arrow ←
    draw.line([(38 * s, 46 * s), (88 * s, 46 * s)], fill=white, width=w)
    draw.polygon([(92 * s, 46 * s), (78 * s, 36 * s), (78 * s, 56 * s)], fill=white)
    draw.line([(40 * s, 68 * s), (90 * s, 68 * s)], fill=white, width=w)
    draw.polygon([(36 * s, 68 * s), (50 * s, 58 * s), (50 * s, 78 * s)], fill=white)
    target = Path("src/main/resources/assets/relaychat/icon.png")
    image.resize((128, 128), Image.LANCZOS).save(target)


if __name__ == "__main__":
    mod_icon()
    print(f"icons written to {OUT}")
