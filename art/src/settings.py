import os, math, numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py"); D=load("dims.py")
GREEN=(57,255,140); DARK=(14,18,22); SLATE=(20,25,30); W=(232,240,236); MUTE=(130,142,150); CARD=(22,28,34); LINE=(40,50,56)
WD,HT=1280,720
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)
_w=open("worldmap.py").read(); _w=_w[:_w.index("# ---- explored mask")]; _ns={}; exec(_w,_ns); TER=_ns["terrain"].convert("RGBA")

img=Image.new("RGBA",(WD,HT),(16,20,25,255))
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
for x in range(0,WD,32): od.line([(x,0),(x,HT)],fill=(57,255,140,7))
for y in range(0,HT,32): od.line([(0,y),(WD,y)],fill=(57,255,140,7))
img.alpha_composite(ov); d=ImageDraw.Draw(img)
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
def tag(x,y,text):
    f=F(12,True); tw=d.textlength(text,font=f)
    d.rectangle([x,y,x+tw+20,y+24],fill=DARK); brackets(x,y,x+tw+20,y+24,6,1); d.text((x+10,y+4),text,font=f,fill=W)
def toggle(x,y,on):
    d.rounded_rectangle([x,y,x+34,y+18],radius=9,fill=(30,90,60) if on else (40,48,54))
    d.ellipse([x+(18 if on else 2),y+2,x+(32 if on else 16),y+16],fill=GREEN if on else MUTE)
def slider(x,y,w,t,label):
    d.line([(x,y+8),(x+w,y+8)],fill=(50,60,66),width=4); d.line([(x,y+8),(x+w*t,y+8)],fill=GREEN,width=4)
    k=x+w*t; d.rectangle([k-6,y,k+6,y+16],fill=DARK,outline=GREEN,width=2)
    d.text((x+w+14,y),label,font=MONO(12),fill=W)
# main rail (Settings active)
RW=60
d.rectangle([0,0,RW,HT],fill=SLATE); d.line([(RW,0),(RW,HT)],fill=(30,110,70))
d.polygon([(RW//2,14),(RW//2+12,26),(RW//2,38),(RW//2-12,26)],outline=GREEN,width=2); d.polygon([(RW//2,21),(RW//2+5,26),(RW//2,31),(RW//2-5,26)],fill=GREEN)
y=60; d.text((10,y),"WORLDS",font=F(9,True),fill=MUTE); y+=16
for n,c in [("overworld",(57,255,140)),("nether",(255,90,70)),("end",(190,120,255)),("modded",(255,190,70))]:
    img.alpha_composite(D["tab"](40,D[n],c),(RW//2-20,y)); y+=48
y+=8; d.line([(12,y),(RW-12,y)],fill=(30,110,70)); y+=12
d.text((10,y),"CODEX",font=F(9,True),fill=MUTE); y+=16
for k,ic in enumerate([G["chip"](40,G["marks"],True),A["act_chip"](40,A["copy"]),A["act_chip"](40,A["printer"]),A["act_chip"](40,A["settings"])]):
    img.alpha_composite(ic,(RW//2-20,y))
    if k==3: d.rectangle([0,y+4,3,y+36],fill=GREEN)
    y+=48
# section list
SX0,SX1=RW+1,RW+190
d.rectangle([SX0,0,SX1,HT],fill=(18,22,27)); d.line([(SX1,0),(SX1,HT)],fill=LINE)
d.text((SX0+18,22),"SETTINGS",font=F(15,True),fill=GREEN)
d.rectangle([SX0+14,52,SX1-14,80],fill=CARD,outline=LINE)
img.alpha_composite(A["act_chip"](20,A["search"]),(SX0+18,56)); d.text((SX0+44,58),"Find a setting…",font=F(12),fill=(110,122,130))
secs=["Minimap","World map","Marks","Sweep","Lenses","Claims","World border","Colors","Motion","Performance","Keys"]
y=100
for i,sname in enumerate(secs):
    on=(i==0)
    if on: d.rectangle([SX0,y-6,SX1-1,y+24],fill=(24,40,34)); d.rectangle([SX0,y-6,SX0+3,y+24],fill=GREEN)
    d.text((SX0+20,y),sname,font=F(13,True if on else False),fill=GREEN if on else (200,208,204)); y+=38
d.text((SX0+18,HT-40),"Changes save as you go.",font=F(11),fill=MUTE)
# content
cx0=SX1+28; PVX=930
d.text((cx0,22),"Minimap",font=F(22,True),fill=W); d.text((cx0,52),"How the minimap looks and what it shows. The preview on the right updates as you change things.",font=F(12),fill=MUTE)
y=90
def row(title,sub=None):
    global y
    d.line([(cx0,y-10),(PVX-30,y-10)],fill=(30,38,44))
    d.text((cx0,y),title,font=F(13,True),fill=W)
    if sub: d.text((cx0,y+18),sub,font=F(11),fill=MUTE)
row("Shape")
fx=cx0+260
for k,(fn,lab) in enumerate([("plate","Codex plate"),("square","Square"),("round","Round")]):
    bx=fx+k*96; on=(k==0)
    d.rectangle([bx,y-4,bx+84,y+80],fill=(24,40,34) if on else CARD,outline=GREEN if on else LINE)
    fr=Image.open(ART+f"frames/frame_{fn}_256.png").convert("RGBA").resize((56,56),Image.LANCZOS); img.alpha_composite(fr,(bx+14,y+2))
    d.text((bx+42-d.textlength(lab,font=F(11))/2,y+62),lab,font=F(11,True) if on else F(11),fill=GREEN if on else W)
y+=104
row("Size"); slider(cx0+260,y+2,260,0.45,"Medium")
y+=44
row("Position","Drag the minimap anywhere on screen")
d.rectangle([cx0+260,y-2,cx0+430,y+28],fill=CARD,outline=GREEN); brackets(cx0+260,y-2,cx0+430,y+28,6,1)
d.text((cx0+274,y+5),"Open layout editor",font=F(12,True),fill=GREEN)
y+=54
row("Turn with the player","Off = north always up"); toggle(cx0+260,y+2,False)
y+=50
row("Zoom"); slider(cx0+260,y+2,260,0.35,"1 : 2")
y+=44
row("Opacity"); slider(cx0+260,y+2,260,0.9,"90%")
y+=44
row("Info lines","Shown under the minimap")
ix=cx0+260
for k,(lab,on) in enumerate([("Coordinates",True),("Biome",True),("Time",False),("Light level",False),("Border distance",True)]):
    bx=ix+(k%3)*130; by=y+(k//3)*28
    d.rectangle([bx,by,bx+16,by+16],fill=(24,60,44) if on else CARD,outline=GREEN if on else MUTE)
    if on: d.line([(bx+3,by+8),(bx+7,by+12),(bx+13,by+4)],fill=GREEN,width=2)
    d.text((bx+24,by),lab,font=F(12),fill=W)
y+=70
row("Hide when","F1, debug screen, or a full menu is open"); toggle(cx0+260,y+2,True)
tag(cx0+300,y+24,"Hides the minimap so it never covers other screens")
# preview panel
d.rectangle([PVX,20,WD-24,HT-24],fill=(12,16,20),outline=LINE); brackets(PVX,20,WD-24,HT-24,14,2)
d.text((PVX+20,36),"PREVIEW",font=F(10,True),fill=MUTE)
S=240
frame=Image.open(ART+"frames/frame_plate_256.png").convert("RGBA").resize((S,S),Image.LANCZOS)
al=np.asarray(frame.getchannel("A")); out=Image.fromarray(((al>140)*255).astype(np.uint8)).copy()
for p in [(0,0),(S-1,0),(0,S-1),(S-1,S-1)]: ImageDraw.floodfill(out,p,128)
inside=Image.fromarray(((np.asarray(out)!=128)*255).astype(np.uint8))
mp=TER.crop((546,240,786,480)).resize((S,S),Image.NEAREST); md=ImageDraw.Draw(mp)
h=Image.open(ART+"marks/mark_home_32.png").convert("RGBA").resize((22,22),Image.LANCZOS); mp.alpha_composite(h,(150,130))
mm=Image.new("RGBA",(S,S),(0,0,0,0)); mm.paste(mp,(0,0),inside)
ar=Image.open(ART+"players/arrow_player_16.png").convert("RGBA").resize((20,20),Image.LANCZOS); mm.alpha_composite(ar,(S//2-10,S//2-10)); mm.alpha_composite(frame)
px=(PVX+WD-24)//2-S//2; img.alpha_composite(mm,(px,64)); d=ImageDraw.Draw(img)
ly=64+S+10
for t in ["1240, 72, -385","Plains","Border: 2,760 blocks"]:
    d.text(((PVX+WD-24)//2-d.textlength(t,font=F(12,True))/2,ly),t,font=F(12,True),fill=W); ly+=18
d.text((PVX+20,HT-90),"Codex plate  ·  Medium  ·  North up",font=F(11),fill=MUTE)
d.text((PVX+20,HT-70),"Reset this section",font=F(12,True),fill=GREEN)
img.convert("RGB").save("ui_settings.png"); print("ok")
