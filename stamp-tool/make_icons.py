from PIL import Image, ImageDraw
from pathlib import Path
import math, sys

ROOT = Path(__file__).resolve().parent

def draw_icon(size):
    im = Image.new("RGBA", (size, size), (10, 65, 49, 255))
    d = ImageDraw.Draw(im)
    m = int(size * .075)
    d.rounded_rectangle((m,m,size-m,size-m), radius=int(size*.22),
                        fill=(15,92,70,255), outline=(236,197,113,255),
                        width=max(2,int(size*.025)))
    x0,y0,x1,y1 = int(size*.23),int(size*.20),int(size*.77),int(size*.79)
    d.rounded_rectangle((x0,y0,x1,y1), radius=int(size*.07), fill=(248,247,238,255))
    fold=int(size*.14)
    d.polygon([(x1-fold,y0),(x1,y0+fold),(x1-fold,y0+fold)], fill=(219,228,216,255))
    line=(15,92,70,255); w=max(2,int(size*.018))
    d.line([(int(size*.32),int(size*.43)),(int(size*.43),int(size*.38)),(int(size*.55),int(size*.44)),(int(size*.68),int(size*.38))], fill=line,width=w)
    d.line([(int(size*.32),int(size*.56)),(int(size*.43),int(size*.51)),(int(size*.55),int(size*.57)),(int(size*.68),int(size*.51))], fill=line,width=w)
    cx,cy=int(size*.52),int(size*.66); r=int(size*.105)
    d.ellipse((cx-r,cy-r,cx+r,cy+r), fill=(202,153,66,255), outline=(132,89,31,255), width=max(2,int(size*.012)))
    pts=[]
    for j in range(10):
        a=-math.pi/2+j*math.pi/5
        rr=r*.72 if j%2==0 else r*.34
        pts.append((cx+rr*math.cos(a),cy+rr*math.sin(a)))
    d.polygon(pts,fill=(255,248,225,255))
    return im

def web():
    draw_icon(192).save(ROOT/"icon-192.png", optimize=True)
    draw_icon(512).save(ROOT/"icon-512.png", optimize=True)

def android():
    res=ROOT/"android/app/src/main/res"
    # Remove Capacitor template vector foregrounds; otherwise Android may prefer
    # the default robot artwork over our custom PNG adaptive-icon foreground.
    for old in res.rglob("ic_launcher_foreground.xml"):
        old.unlink()
    # Legacy launcher icons for older Android launchers.
    for density, size in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
        folder=res/f"mipmap-{density}"; folder.mkdir(parents=True,exist_ok=True)
        draw_icon(size).save(folder/"ic_launcher.png", optimize=True)
        draw_icon(size).save(folder/"ic_launcher_round.png", optimize=True)
    # Adaptive launcher icon foreground with transparent safe area.
    fg=Image.new("RGBA",(432,432),(0,0,0,0))
    mark=draw_icon(320)
    fg.alpha_composite(mark,(56,56))
    nd=res/"drawable"; nd.mkdir(parents=True,exist_ok=True)
    fg.save(nd/"ic_launcher_foreground.png", optimize=True)
    bg=res/"values"; bg.mkdir(parents=True,exist_ok=True)
    (bg/"ic_launcher_background.xml").write_text(
        '<resources><color name="ic_launcher_background">#0A4131</color></resources>', encoding="utf-8")
    anydpi=res/"mipmap-anydpi-v26"; anydpi.mkdir(parents=True,exist_ok=True)
    xml='''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@drawable/ic_launcher_foreground"/>
</adaptive-icon>'''
    (anydpi/"ic_launcher.xml").write_text(xml,encoding="utf-8")
    (anydpi/"ic_launcher_round.xml").write_text(xml,encoding="utf-8")

if __name__ == "__main__":
    mode=sys.argv[1] if len(sys.argv)>1 else "all"
    if mode in ("web","all"): web()
    if mode in ("android","all"): android()
