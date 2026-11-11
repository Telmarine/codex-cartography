import os, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py"); D=load("dims.py")
GREEN=(57,255,140); DARK=(14,18,22); SLATE=(20,25,30); W=(232,240,236); MUTE=(130,142,150); CARD=(22,28,34); LINE=(40,50,56); RED=(255,80,80)
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
def tag(x,y,text,c=GREEN):
    f=F(12,True); tw=d.textlength(text,font=f)
    d.rectangle([x,y,x+tw+20,y+24],fill=DARK); brackets(x,y,x+tw+20,y+24,6,1,c); d.text((x+10,y+4),text,font=f,fill=W)
# rail: map data lives under Settings? give it the Print slot? -> its own glyph: a stack (copy) is Folios; use a disk glyph
def disk(dd,c,u,f):
    for i,yo in enumerate([-4,0,4]):
        dd.ellipse([c-6*u,c+yo*u-2*u,c+6*u,c+yo*u+2*u],outline=f,width=int(1.0*u))
    dd.line([(c-6*u,c-4*u),(c-6*u,c+4*u)],fill=f,width=int(1.0*u)); dd.line([(c+6*u,c-4*u),(c+6*u,c+4*u)],fill=f,width=int(1.0*u))
RW=60
d.rectangle([0,0,RW,HT],fill=SLATE); d.line([(RW,0),(RW,HT)],fill=(30,110,70))
d.polygon([(RW//2,14),(RW//2+12,26),(RW//2,38),(RW//2-12,26)],outline=GREEN,width=2); d.polygon([(RW//2,21),(RW//2+5,26),(RW//2,31),(RW//2-5,26)],fill=GREEN)
y=60; d.text((10,y),"WORLDS",font=F(9,True),fill=MUTE); y+=16
for n,c in [("overworld",(57,255,140)),("nether",(255,90,70)),("end",(190,120,255)),("modded",(255,190,70))]:
    img.alpha_composite(D["tab"](40,D[n],c),(RW//2-20,y)); y+=48
y+=8; d.line([(12,y),(RW-12,y)],fill=(30,110,70)); y+=12
d.text((10,y),"CODEX",font=F(9,True),fill=MUTE); y+=16
for k,ic in enumerate([G["chip"](40,G["marks"],True),A["act_chip"](40,A["copy"]),A["act_chip"](40,A["printer"]),A["act_chip"](40,disk),A["act_chip"](40,A["settings"])]):
    img.alpha_composite(ic,(RW//2-20,y))
    if k==3: d.rectangle([0,y+4,3,y+36],fill=GREEN)
    y+=48
hx0,hy0=RW+20,16
d.rectangle([hx0,hy0,hx0+380,hy0+44],fill=DARK); brackets(hx0,hy0,hx0+380,hy0+44)
img.alpha_composite(A["act_chip"](28,disk),(hx0+10,hy0+8))
d.text((hx0+46,hy0+6),"MAP DATA",font=F(15,True),fill=GREEN); d.text((hx0+46,hy0+25),"This server only  ·  960 MB on disk",font=F(11),fill=MUTE)
d.text((hx0,84),"The map saved for the server you're on. Other servers and worlds are never shown here.",font=F(12),fill=MUTE)
# left column: preview + name + open
L0,L1=hx0,hx0+520
th=TER.crop((420,180,940,520)).resize((L1-L0,300),Image.NEAREST); img.alpha_composite(th,(L0,112))
d.rectangle([L0,112,L1,412],outline=(30,110,70)); brackets(L0,112,L1,412,14,2)
d.text((L0,428),"play.tenkominecraft.com",font=F(18,True),fill=W); d.text((L0,454),"Server  ·  first mapped 12 Oct 2026  ·  last visited today",font=F(12),fill=MUTE)
d.text((L0,488),"NAME SHOWN ON THE MAP",font=F(10,True),fill=MUTE)
d.rectangle([L0,504,L1-44,534],fill=CARD,outline=LINE); d.text((L0+10,511),"Tenko",font=F(13,True),fill=W)
img.alpha_composite(A["act_chip"](30,A["edit"]),(L1-34,504))
d.text((L0,552),"EXPLORED",font=F(10,True),fill=MUTE)
for k,(lab,val) in enumerate([("Regions","412"),("Chunks","98,304"),("Marks","41"),("Folios","6")]):
    bx=L0+k*130; d.rectangle([bx,568,bx+120,616],fill=CARD,outline=LINE)
    d.text((bx+10,574),lab,font=F(11),fill=MUTE); d.text((bx+10,592),val,font=MONO(14),fill=GREEN)
# right column: dimensions + delete
R0,R1=L1+40,WD-28
d.text((R0,112),"DIMENSIONS",font=F(10,True),fill=MUTE)
dims=[("overworld","Overworld","610 MB",0.64,(57,255,140)),("nether","The Nether","240 MB",0.25,(255,90,70)),("end","The End","110 MB",0.11,(190,120,255))]
for k,(dm,lab,sz,fr,c2) in enumerate(dims):
    ry=130+k*72
    d.rectangle([R0,ry,R1,ry+62],fill=CARD,outline=LINE)
    img.alpha_composite(D["tab"](28,D[dm],c2),(R0+10,ry+10)); d.text((R0+48,ry+12),lab,font=F(14,True),fill=W)
    d.text((R1-110-d.textlength(sz,font=MONO(12)),ry+14),sz,font=MONO(12),fill=MUTE)
    d.rectangle([R0+48,ry+40,R1-60,ry+46],fill=(30,38,44)); d.rectangle([R0+48,ry+40,R0+48+(R1-108-R0)*fr,ry+46],fill=c2)
    rm=A["act_chip"](32,A["remove"],RED) if k==2 else A["act_chip"](32,A["remove"])
    img.alpha_composite(rm,(R1-44,ry+15))
wy=352
d.rectangle([R0,wy,R1,wy+112],fill=(40,18,18),outline=RED); brackets(R0,wy,R1,wy+112,12,2,RED)
d.polygon([(R0+30,wy+16),(R0+46,wy+44),(R0+14,wy+44)],fill=(255,210,60)); d.rectangle([R0+29,wy+24,R0+31,wy+35],fill=DARK); d.rectangle([R0+29,wy+38,R0+31,wy+40],fill=DARK)
d.text((R0+60,wy+14),"Delete The End's map?",font=F(14,True),fill=(255,190,190))
d.text((R0+60,wy+36),"This removes the explored map of The End saved on this computer.",font=F(12),fill=W)
d.text((R0+60,wy+54),"Marks and folios stay. This can't be undone.",font=F(12),fill=W)
d.text((R0+60,wy+80),"Click the red delete again to confirm",font=F(12,True),fill=(255,170,170))
d.text((R1-16-d.textlength("Cancel (Esc)",font=F(12,True)),wy+80),"Cancel (Esc)",font=F(12,True),fill=GREEN)
ay=HT-90
d.rectangle([R0,ay,R1,ay+52],fill=(40,18,18),outline=RED)
img.alpha_composite(A["act_chip"](32,A["remove"],RED),(R0+10,ay+10)); d.text((R0+54,ay+9),"Delete this server's whole map",font=F(14,True),fill=(255,170,170))
d.text((R0+54,ay+29),"Explored map, marks and folios on this computer. Asks to confirm.",font=F(11),fill=(200,140,140))
img.convert("RGB").save("ui_map_data.png"); print("ok")
