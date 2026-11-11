import os, math
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
# rail (Folios active)
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
    if k==1: d.rectangle([0,y+4,3,y+36],fill=GREEN)
    y+=48
# header
hx0,hy0=RW+20,16; DX=900
d.rectangle([hx0,hy0,hx0+300,hy0+44],fill=DARK); brackets(hx0,hy0,hx0+300,hy0+44)
img.alpha_composite(A["act_chip"](28,A["copy"]),(hx0+10,hy0+8))
d.text((hx0+46,hy0+6),"FOLIOS",font=F(15,True),fill=GREEN); d.text((hx0+46,hy0+25),"6 folios  ·  41 marks",font=F(11),fill=MUTE)
# header buttons: import + new folio
bx=DX-20-44; d.rectangle([bx,hy0,bx+44,hy0+44],fill=DARK); brackets(bx,hy0,bx+44,hy0+44)
img.alpha_composite(A["act_chip"](28,A["newmark"]),(bx+8,hy0+8))
ix=bx-12-150; d.rectangle([ix,hy0,ix+150,hy0+44],fill=DARK); brackets(ix,hy0,ix+150,hy0+44)
# import glyph: arrow into a tray
gN=28*8; gi=Image.new("RGBA",(gN,gN),(0,0,0,0)); gd=ImageDraw.Draw(gi); c=gN/2; u=gN*0.04
gd.line([(c,c-7*u),(c,c+2*u)],fill=W,width=int(1.4*u)); gd.polygon([(c,c+4.5*u),(c-3.2*u,c+0.8*u),(c+3.2*u,c+0.8*u)],fill=W)
gd.line([(c-7*u,c+3*u),(c-7*u,c+7*u),(c+7*u,c+7*u),(c+7*u,c+3*u)],fill=W,width=int(1.2*u))
img.alpha_composite(gi.resize((28,28),Image.LANCZOS),(ix+10,hy0+8))
d.text((ix+46,hy0+14),"Import waypoints",font=F(12,True),fill=W)
# folio cards
folios=[("Base",(57,255,140),["home","keep","farm","village"],9,True),
("Mining",(255,154,46),["mine","question","mine","treasure"],7,True),
("Travel",(70,170,255),["flag","portal","gate","flag"],6,True),
("Adventure",(255,210,60),["skull","ruin","shrine","treasure"],11,True),
("Nether trips",(255,90,70),["portal","star","danger","skull"],5,False),
("From JourneyMap",(160,166,172),["question","question","flag","home"],3,True)]
CW,CH,GAP=252,150,14; gx0,gy0=hx0,84
d.rectangle([gx0,gy0-6,gx0+784,gy0+40],fill=(18,30,26),outline=(30,90,60))
d.text((gx0+12,gy0),"Folios are folders for your marks, sorted however you like (Base, Mining, Travel…).",font=F(12,True),fill=W)
d.text((gx0+12,gy0+18),"Turn a folio off to hide all its marks at once. Marks with no folio still show in the Ledger.",font=F(12),fill=MUTE)
gy0+=56
for i,(name,col,ems,cnt,on) in enumerate(folios):
    cx=gx0+(i%3)*(CW+GAP); cy=gy0+(i//3)*(CH+GAP); sel=(i==1)
    d.rectangle([cx,cy,cx+CW,cy+CH],fill=(30,28,22) if sel else CARD,outline=col if sel else LINE)
    d.rectangle([cx,cy,cx+5,cy+CH],fill=col if on else (70,78,84))
    if sel: brackets(cx-3,cy-3,cx+CW+3,cy+CH+3,14,2,col)
    d.text((cx+20,cy+14),name,font=F(15,True),fill=W if on else MUTE)
    d.text((cx+20,cy+36),f"{cnt} marks",font=F(12),fill=MUTE)
    toggle(cx+CW-50,cy+16,on)
    for k,e in enumerate(ems):
        ic=Image.open(ART+f"marks/mark_{e}_64.png").convert("RGBA").resize((40,40),Image.LANCZOS)
        if not on: ic.putalpha(ic.getchannel("A").point(lambda v:v//3))
        img.alpha_composite(ic,(cx+16+k*46,cy+70))
    if cnt>4: d.text((cx+16+4*46+4,cy+80),f"+{cnt-4}",font=F(13,True),fill=MUTE)
    if not on: d.text((cx+20,cy+122),"Hidden",font=F(11,True),fill=(150,120,100))
tag(gx0+(CW+GAP)*1+CW-230,gy0+CH+GAP+40,"Turn the whole folio on or off")
# drawer
d.rectangle([DX,0,WD,HT],fill=(12,16,20)); d.line([(DX,0),(DX,HT)],fill=GREEN,width=2)
g=Image.new("RGBA",(WD,HT),(0,0,0,0)); ImageDraw.Draw(g).rectangle([DX-6,0,DX+2,HT],fill=(57,255,140,60))
img.alpha_composite(g.filter(ImageFilter.GaussianBlur(5))); d=ImageDraw.Draw(img)
px=DX+28; pr=WD-28; OR=(255,154,46)
d.rectangle([px,30,px+56,86],fill=CARD,outline=OR); d.rectangle([px,30,px+8,86],fill=OR)
d.text((px+74,30),"Mining",font=F(24,True),fill=W); d.text((px+74,64),"7 marks  ·  Overworld 6, Nether 1",font=F(12),fill=MUTE)
d.line([(px,108),(pr,108)],fill=LINE)
y=122; d.text((px,y),"COLOR",font=F(10,True),fill=MUTE)
d.rectangle([px,y+18,px+28,y+46],fill=OR,outline=W); d.text((px+38,y+23),"#FF9A2E",font=MONO(13),fill=W); d.text((px+120,y+23),"Change…",font=F(12,True),fill=GREEN)
d.text((px+210,y),"OPTIONS",font=F(10,True),fill=MUTE)
toggle(px+210,y+18,True); d.text((px+252,y+19),"Shown",font=F(13),fill=W)
y=180; d.text((px,y),"WHAT THE FOLIO COLOR CHANGES",font=F(10,True),fill=MUTE)
for k,(t,on) in enumerate([("Just the folio (marks keep their own colors)",True),("Every mark in this folio",False)]):
    ry=y+18+k*22; d.ellipse([px,ry,px+16,ry+16],outline=GREEN if on else MUTE,width=2)
    if on: d.ellipse([px+4,ry+4,px+12,ry+12],fill=GREEN)
    d.text((px+24,ry),t,font=F(12),fill=W)
y=248; d.text((px,y),"MARKS",font=F(10,True),fill=MUTE)
rows=[("mine","Iron Mine","318m","overworld"),("mine","Copper Vein","274m","overworld"),("question","Odd Cave","1.1k","overworld"),
("treasure","Gold Cache","1.6k","overworld"),("mine","Deep Shaft","2.0k","overworld"),("mine","Quartz Wall","3.4k","nether")]
for k,(e,n,dist,dm) in enumerate(rows[:5]):
    ry=y+18+k*44
    d.rectangle([px,ry,pr,ry+38],fill=CARD,outline=LINE)
    ic=Image.open(ART+f"marks/mark_{e}_32.png").convert("RGBA"); img.alpha_composite(ic,(px+4,ry+3))
    d.text((px+44,ry+10),n,font=F(13,True),fill=W)
    img.alpha_composite(D["tab"](20,D[dm],(57,255,140) if dm=="overworld" else (255,90,70)),(pr-90,ry+9))
    d.text((pr-62,ry+11),dist,font=MONO(12),fill=GREEN)
d.text((px,y+18+5*44+4),"+2 more",font=F(12),fill=MUTE)
ay=HT-74; d.line([(px,ay-14),(pr,ay-14)],fill=LINE)
for k,f in enumerate([A["edit"],A["hide"],A["remove"]]): img.alpha_composite(A["act_chip"](40,f),(px+k*56,ay))
d.text((px+180,ay+4),"Removing a folio keeps",font=F(11),fill=MUTE); d.text((px+180,ay+20),"its marks (no folio).",font=F(11),fill=MUTE)
img.convert("RGB").save("ui_folios.png"); print("ok")
