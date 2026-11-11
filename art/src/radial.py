import os, math, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py")
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); MUTE=(130,142,150)
WD,HT=1280,720
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)

# ---------- a plain blocky scene (not game art: our own stand-in)
rng=np.random.default_rng(3)
img=Image.new("RGBA",(WD,HT)); a=np.zeros((HT,WD,4),dtype=np.uint8)
for y in range(HT):
    t=y/HT; a[y,:,:3]=np.array([120,170,220])*(1-t)+np.array([190,215,235])*t; a[y,:,3]=255
img=Image.fromarray(a); d=ImageDraw.Draw(img)
B=24
def hills(base,amp,freq,col,seed):
    r=np.random.default_rng(seed); ph=r.random()*6
    for x in range(0,WD,B):
        h=base-int((amp*math.sin(x*freq+ph)+amp*0.5*math.sin(x*freq*2.3+ph*2))/B)*B
        d.rectangle([x,h,x+B-1,HT],fill=col)
        d.rectangle([x,h,x+B-1,h+5],fill=tuple(min(255,c+18) for c in col))
hills(360,60,0.004,(132,160,176),1)
hills(420,50,0.006,(96,132,110),2)
hills(500,40,0.009,(78,120,72),3)
# near ground with block grid
for x in range(0,WD,48):
    h=560+int(12*math.sin(x*0.02))//48*48
    for y in range(h,HT,48):
        top=(y==h)
        c=(92,150,70) if top else (122,88,60)
        c=tuple(int(v*(0.92+0.16*rng.random())) for v in c)
        d.rectangle([x,y,x+47,y+47],fill=c); d.rectangle([x,y,x+47,y+47],outline=tuple(int(v*0.85) for v in c))
        if top: d.rectangle([x,y+30,x+47,y+47],fill=(122,88,60))
# a few block trees
for tx in [180,980,1120]:
    ty=520
    d.rectangle([tx,ty-120,tx+24,ty+40],fill=(100,74,48))
    for (ox,oy) in [(-48,-168),(0,-168),(-24,-216),(24,-120),(-72,-120),(-48,-120)]:
        d.rectangle([tx+ox,ty+oy,tx+ox+71,ty+oy+47],fill=(58,110,52)); d.rectangle([tx+ox,ty+oy,tx+ox+71,ty+oy+47],outline=(48,92,44))
img=img.convert("RGBA")

# ---------- minimap (top-left) in the plate frame
_w=open("worldmap.py").read(); _w=_w[:_w.index("# ---- explored mask")]; _ns={}; exec(_w,_ns)
mapsrc=_ns["terrain"].convert("RGBA").crop((566,260,766,460))
_md=ImageDraw.Draw(mapsrc)
for gx in range(-6,200,64): _md.line([(gx,0),(gx,200)],fill=(255,255,255,30))
_h=Image.open(ART+"marks/mark_home_32.png").convert("RGBA").resize((20,20),Image.LANCZOS); mapsrc.alpha_composite(_h,(130,112))
frame=Image.open(ART+"frames/frame_plate_256.png").convert("RGBA").resize((200,200),Image.LANCZOS)
al=np.asarray(frame.getchannel("A"))
out=Image.fromarray(((al>140)*255).astype(np.uint8)).copy(); out.load(); [ImageDraw.floodfill(out,p,128) for p in [(0,0),(199,0),(0,199),(199,199)]]
inside=Image.fromarray(((np.asarray(out)!=128)*255).astype(np.uint8))
mm=Image.new("RGBA",(200,200),(0,0,0,0)); mm.paste(mapsrc,(0,0),inside)
arrow=Image.open(ART+"players/arrow_player_16.png").convert("RGBA").rotate(-35,resample=Image.BICUBIC,expand=True)
mm.alpha_composite(arrow,(100-arrow.width//2,100-arrow.height//2)); mm.alpha_composite(frame)
img.alpha_composite(mm,(16,16)); d=ImageDraw.Draw(img)
d.text((24,222),"1240, 72, -385  ·  Plains",font=F(12,True),fill=W,stroke_width=2,stroke_fill=(10,14,18))

# ---------- dim the game behind the radial
img=Image.alpha_composite(img,Image.new("RGBA",(WD,HT),(6,10,14,120))); d=ImageDraw.Draw(img)

# ---------- glyph helper (no chip)
def glyph(fn,size,col=W):
    N=size*8; im=Image.new("RGBA",(N,N),(0,0,0,0)); dd=ImageDraw.Draw(im); fn(dd,N/2,N*0.04,col); return im.resize((size,size),Image.LANCZOS)
def minimap(d,c,u,f):
    m=6*u; d.rectangle([c-m,c-m,c+m,c+m],outline=f,width=int(1.2*u))
    d.polygon([(c,c-3.2*u),(c+2.4*u,c+3*u),(c,c+1.6*u),(c-2.4*u,c+3*u)],fill=f)
def zoom(sign):
    def g(d,c,u,f):
        r=4.2*u; ox=c-1.2*u; oy=c-1.2*u
        d.ellipse([ox-r,oy-r,ox+r,oy+r],outline=f,width=int(1.3*u))
        d.line([(ox+r*0.7,oy+r*0.7),(c+6*u,c+6*u)],fill=f,width=int(1.6*u))
        d.line([(ox-2.2*u,oy),(ox+2.2*u,oy)],fill=f,width=int(1.1*u))
        if sign>0: d.line([(ox,oy-2.2*u),(ox,oy+2.2*u)],fill=f,width=int(1.1*u))
    return g
def depth(d,c,u,f):
    for i,yo in enumerate([-4.5,-1.2,2.1]):
        y=c+yo*u; d.polygon([(c-6*u,y),(c,y-2.2*u),(c+6*u,y),(c,y+2.2*u)],outline=f if i<2 else None,fill=None if i<2 else f,width=int(0.9*u))
    d.line([(c+6.4*u,c-4*u),(c+6.4*u,c+5.6*u)],fill=f,width=int(0.9*u))
    d.polygon([(c+6.4*u,c+6.8*u),(c+5*u,c+4.6*u),(c+7.8*u,c+4.6*u)],fill=f)
slices=[("New mark here",A["newmark"]),("Ledger",G["marks"]),("Zoom in",zoom(1)),("Zoom out",zoom(-1)),
        ("Lens: Surface",A["lens"]),("Depth View",depth),("Minimap: on",minimap),("Settings",A["settings"])]
cx,cy=WD//2,HT//2; R0,R1=78,200; n=len(slices); gap=2.2
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
sel=0
for i,(name,fn) in enumerate(slices):
    a0=-90-180/n+i*360/n+gap; a1=a0+360/n-2*gap
    pts=[(cx+R1*math.cos(math.radians(a)),cy+R1*math.sin(math.radians(a))) for a in np.linspace(a0,a1,30)]
    a0i=a0+gap*R1/R0*0.5; a1i=a1-gap*R1/R0*0.5
    pts+= [(cx+R0*math.cos(math.radians(a)),cy+R0*math.sin(math.radians(a))) for a in np.linspace(a1i,a0i,20)]
    on=(i==sel)
    od.polygon(pts,fill=(24,52,40,235) if on else (14,18,22,225),outline=GREEN+(255,) if on else (57,255,140,70),width=2 if on else 1)
img.alpha_composite(ov)
if True:
    a=math.radians(-90); g=Image.new("RGBA",(WD,HT),(0,0,0,0)); gd=ImageDraw.Draw(g)
    gd.arc([cx-R1-2,cy-R1-2,cx+R1+2,cy+R1+2],-90-180/n+gap,-90+180/n-gap,fill=GREEN+(200,),width=6)
    img.alpha_composite(g.filter(ImageFilter.GaussianBlur(4)))
d=ImageDraw.Draw(img)
for i,(name,fn) in enumerate(slices):
    am=math.radians(-90+i*360/n); rm=(R0+R1)/2
    gx,gy=cx+rm*math.cos(am),cy+rm*math.sin(am)
    on=(i==sel)
    gi=glyph(fn,64,GREEN if on else W); img.alpha_composite(gi,(int(gx-32),int(gy-46)))
    f=F(11,True); tw=d.textlength(name,font=f)
    d.text((gx-tw/2,gy+20),name,font=f,fill=GREEN if on else (200,210,206))
# outer bracket ring accents
for k in range(4):
    a=math.radians(45+k*90); x,y=cx+(R1+12)*math.cos(a),cy+(R1+12)*math.sin(a)
    d.line([(x-8*math.copysign(1,math.cos(a)),y),(x,y)],fill=GREEN,width=2); d.line([(x,y-8*math.copysign(1,math.sin(a))),(x,y)],fill=GREEN,width=2)
# center
d.ellipse([cx-R0+8,cy-R0+8,cx+R0-8,cy+R0-8],fill=(10,14,18,240),outline=(57,255,140,90))
d.polygon([(cx,cy-R0+20),(cx+7,cy-R0+28),(cx,cy-R0+36),(cx-7,cy-R0+28)],fill=GREEN)  # pointer toward picked slice
t1="New mark here"; d.text((cx-d.textlength(t1,font=F(13,True))/2,cy-10),t1,font=F(13,True),fill=W)
t2="Release to pick"; d.text((cx-d.textlength(t2,font=F(10))/2,cy+10),t2,font=F(10),fill=MUTE)
# crosshair hidden while open; hint at bottom
hint="Hold the Codex key  ·  move the mouse to a slice  ·  release"
d.text((cx-d.textlength(hint,font=F(12))/2,cy+R1+34),hint,font=F(12),fill=(200,210,206))
img.convert("RGB").save("ui_radial.png"); print("ok")
