import os, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py"); D=load("dims.py")
GREEN=(57,255,140); DARK=(14,18,22); SLATE=(20,25,30); W=(232,240,236); MUTE=(130,142,150); CARD=(22,28,34)
WD,HT=1280,720
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)

img=Image.new("RGBA",(WD,HT),(16,20,25,255))
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
for x in range(0,WD,32): od.line([(x,0),(x,HT)],fill=(57,255,140,7))
for y in range(0,HT,32): od.line([(0,y),(WD,y)],fill=(57,255,140,7))
img.alpha_composite(ov)
d=ImageDraw.Draw(img)
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
def tag(x,y,text):
    f=F(12,True); tw=d.textlength(text,font=f)
    d.rectangle([x,y,x+tw+20,y+24],fill=DARK); brackets(x,y,x+tw+20,y+24,6,1); d.text((x+10,y+4),text,font=f,fill=W)

# ---- left index rail (Ledger active)
RW=60
d.rectangle([0,0,RW,HT],fill=SLATE); d.line([(RW,0),(RW,HT)],fill=(30,110,70),width=1)
d.polygon([(RW//2,14),(RW//2+12,26),(RW//2,38),(RW//2-12,26)],outline=GREEN,width=2); d.polygon([(RW//2,21),(RW//2+5,26),(RW//2,31),(RW//2-5,26)],fill=GREEN)
y=60; d.text((10,y),"WORLDS",font=F(9,True),fill=MUTE); y+=16
for n,c in [("overworld",(57,255,140)),("nether",(255,90,70)),("end",(190,120,255)),("modded",(255,190,70))]:
    img.alpha_composite(D["tab"](40,D[n],c),(RW//2-20,y)); y+=48
y+=8; d.line([(12,y),(RW-12,y)],fill=(30,110,70)); y+=12
d.text((10,y),"CODEX",font=F(9,True),fill=MUTE); y+=16
ledger_y=y
for ic in [G["chip"](40,G["marks"],True),A["act_chip"](40,A["copy"]),A["act_chip"](40,A["printer"]),A["act_chip"](40,A["settings"])]:
    img.alpha_composite(ic,(RW//2-20,y)); y+=48
d.rectangle([0,ledger_y+4,3,ledger_y+36],fill=GREEN)

# ---- header
hx0,hy0=RW+20,16
d.rectangle([hx0,hy0,hx0+300,hy0+44],fill=DARK); brackets(hx0,hy0,hx0+300,hy0+44)
img.alpha_composite(G["chip"](28,G["marks"],True),(hx0+10,hy0+8))
d.text((hx0+46,hy0+6),"LEDGER",font=F(15,True),fill=GREEN)
d.text((hx0+46,hy0+25),"17 marks  ·  4 folios",font=F(11),fill=MUTE)
DX=900   # drawer left edge
sx=DX-20-220-56
d.rectangle([sx,hy0,sx+220,hy0+44],fill=DARK); brackets(sx,hy0,sx+220,hy0+44)
img.alpha_composite(A["act_chip"](28,A["search"]),(sx+8,hy0+8))
d.text((sx+44,hy0+14),"Search marks…",font=F(12),fill=(110,122,130))
nx=sx+232
d.rectangle([nx,hy0,nx+44,hy0+44],fill=DARK); brackets(nx,hy0,nx+44,hy0+44)
img.alpha_composite(A["act_chip"](28,A["newmark"]),(nx+8,hy0+8))

# ---- filter chips
fy=76; x=hx0
def chip(label,on,icon=None):
    global x
    f=F(12,True); tw=d.textlength(label,font=f); w=int(tw)+20+(24 if icon else 0)
    d.rectangle([x,fy,x+w,fy+28],fill=(26,40,34) if on else CARD,outline=GREEN if on else (52,62,70))
    if icon: img.alpha_composite(D["tab"](20,D[icon[0]],icon[1]),(x+6,fy+4))
    d.text((x+10+(24 if icon else 0),fy+6),label,font=f,fill=GREEN if on else W); x+=w+8
chip("All",True); chip("Overworld",False,("overworld",GREEN)); chip("Nether",False,("nether",(255,90,70))); chip("End",False,("end",(190,120,255)))
x+=10; d.line([(x-9,fy+4),(x-9,fy+24)],fill=(52,62,70))
for fo in ["Base","Travel","Adventure","Mining"]: chip(fo,False)
def caret(x,y,c=MUTE): d.polygon([(x,y),(x+8,y),(x+4,y+5)],fill=c)
sx2=DX-34-d.textlength("Sort: Nearest",font=F(12)); d.text((sx2,fy+6),"Sort: Nearest",font=F(12),fill=MUTE); caret(DX-28,fy+12)

# ---- cards
marks=[("home","Home","142m","overworld","Base","1240, 72, -385"),
("keep","Tempest's Hold","160m","overworld","Base","1302, 70, -410"),
("farm","Wheat Fields","210m","overworld","Base","1180, 68, -520"),
("death","Death point","290m","overworld","","1105, 31, -122"),
("mine","Iron Mine","318m","overworld","Mining","960, 12, -240"),
("flag","Meeting Point","388m","overworld","Travel","1580, 74, -190"),
("portal","Nether Portal","402m","overworld","Travel","1410, 66, 2"),
("skull","Spawner","455m","overworld","Adventure","820, 24, -610"),
("danger","Lava Lake","530m","overworld","","1640, 11, -760"),
("ruin","Old Column","610m","overworld","Adventure","700, 80, -20"),
("shrine","Hill Shrine","840m","overworld","Adventure","2010, 96, -300"),
("question","Odd Cave","1.1k","overworld","Mining","400, 40, -900"),
("treasure","Buried Loot","1.2k","overworld","Adventure","2400, 62, -40"),
("ancient_city","Ancient City","1.9k","overworld","","-560, -51, 380"),
("ocean_monument","Ocean Monument","2.4k","overworld","","3500, 50, -800"),
]
CW,CH,GAP=256,84,14; gx0,gy0=hx0,124
for i,(em,name,dist,dim,fol,xyz) in enumerate(marks):
    cx=gx0+(i%3)*(CW+GAP); cy=gy0+(i//3)*(CH+GAP)
    sel=(i==0)
    d.rectangle([cx,cy,cx+CW,cy+CH],fill=(24,36,32) if sel else CARD,outline=(57,255,140) if sel else (40,50,56))
    if sel: brackets(cx-3,cy-3,cx+CW+3,cy+CH+3,14,2)
    ic=Image.open(ART+f"marks/mark_{em}_64.png").convert("RGBA").resize((52,52),Image.LANCZOS)
    img.alpha_composite(ic,(cx+12,cy+(CH-52)//2))
    tx=cx+76
    d.text((tx,cy+12),name,font=F(14,True),fill=W)
    d.text((tx,cy+34),dist,font=MONO(12),fill=GREEN)
    img.alpha_composite(D["tab"](20,D[dim],GREEN),(int(tx+d.textlength(dist,font=MONO(12))+10),cy+31))
    if fol: d.text((tx+d.textlength(dist,font=MONO(12))+38,cy+34),fol,font=F(12),fill=MUTE)
    d.text((tx,cy+56),xyz,font=MONO(11),fill=(100,112,120))
# scroll hint
sy=gy0+5*(CH+GAP)
d.text((gx0,sy+2),"2 more",font=F(12),fill=MUTE); caret(gx0+50,sy+8)
# scroll bar
d.rectangle([DX-12,gy0,DX-9,gy0+5*(CH+GAP)-GAP],fill=(30,38,44)); d.rectangle([DX-12,gy0,DX-9,gy0+380],fill=(57,255,140))

# ---- detail drawer
d.rectangle([DX,0,WD,HT],fill=(12,16,20)); d.line([(DX,0),(DX,HT)],fill=GREEN,width=2)
glow=Image.new("RGBA",(WD,HT),(0,0,0,0)); gd=ImageDraw.Draw(glow); gd.rectangle([DX-6,0,DX+2,HT],fill=(57,255,140,60))
img.alpha_composite(glow.filter(ImageFilter.GaussianBlur(5))); d=ImageDraw.Draw(img)
px=DX+28
big=Image.open(ART+"marks/mark_home_64.png").convert("RGBA").resize((88,88),Image.LANCZOS)
img.alpha_composite(big,(px-8,24))
d.text((px+92,36),"Home",font=F(24,True),fill=W)
d.text((px+92,70),"Base folio  ·  made by you",font=F(12),fill=MUTE)
d.line([(px,130),(WD-28,130)],fill=(40,50,56))
def field(y,label):
    d.text((px,y),label.upper(),font=F(10,True),fill=MUTE)
fy=146
field(fy,"Coordinates")
for j,(k,v) in enumerate([("X","1240"),("Y","72"),("Z","-385")]):
    bx=px+j*92
    d.rectangle([bx,fy+18,bx+84,fy+46],fill=CARD,outline=(40,50,56)); d.text((bx+8,fy+24),k,font=F(12,True),fill=GREEN); d.text((bx+24,fy+24),v,font=MONO(13),fill=W)
img.alpha_composite(A["act_chip"](28,A["copy"]),(px+280,fy+18))
fy+=66; field(fy,"Dimension")
img.alpha_composite(D["tab"](24,D["overworld"],GREEN),(px,fy+18)); d.text((px+32,fy+21),"Overworld",font=F(13,True),fill=W)
field2x=px+180; d.text((field2x,fy),"DISTANCE",font=F(10,True),fill=MUTE)
d.text((field2x,fy+21),"142m",font=MONO(13),fill=GREEN)
ax,ay=field2x+64,fy+29; ang=math.radians(-45)
pts=[(ax+10*math.cos(ang),ay+10*math.sin(ang)),(ax+6*math.cos(ang+2.5),ay+6*math.sin(ang+2.5)),(ax+6*math.cos(ang-2.5),ay+6*math.sin(ang-2.5))]
d.polygon(pts,fill=GREEN); d.text((ax+14,fy+21),"NE",font=F(12),fill=MUTE)
fy+=58; field(fy,"Color")
d.rectangle([px,fy+18,px+28,fy+46],fill=GREEN,outline=W); d.text((px+38,fy+23),"#39FF8C",font=MONO(13),fill=W)
d.text((px+120,fy+23),"Change…",font=F(12,True),fill=GREEN)
fy+=66; field(fy,"Folio")
d.rectangle([px,fy+18,px+300,fy+46],fill=CARD,outline=(40,50,56)); d.text((px+10,fy+24),"Base",font=F(13,True),fill=W); caret(px+282,fy+30)
fy+=66; field(fy,"Options")
def toggle(y,label,on):
    d.rounded_rectangle([px,y,px+34,y+18],radius=9,fill=(30,90,60) if on else (40,48,54))
    d.ellipse([px+(18 if on else 2),y+2,px+(32 if on else 16),y+16],fill=GREEN if on else MUTE)
    d.text((px+44,y+1),label,font=F(13),fill=W)
toggle(fy+20,"Show in the world",True); toggle(fy+46,"Show on the minimap",True); toggle(fy+72,"Show distance",True)
# actions
ay0=HT-86
d.line([(px,ay0-16),(WD-28,ay0-16)],fill=(40,50,56))
acts=[A["act_chip"](40,A["travel"]),A["act_chip"](40,A["share"]),A["act_chip"](40,A["edit"]),A["act_chip"](40,A["hide"]),A["act_chip"](40,A["remove"])]
for j,ic in enumerate(acts): img.alpha_composite(ic,(px+j*56,ay0))
tag(px+56-8,ay0+48,"Share")
# hover on card (another example)
tag(gx0+150,gy0+(CH+GAP)*2+58,"Discovered portal")
img.convert("RGB").save("ui_ledger.png"); print("ok")
