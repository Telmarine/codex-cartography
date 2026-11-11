import os, math
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
G=load("glyphs.py"); A=load("actions.py")
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); MUTE=(130,142,150); CARD=(22,28,34); LINE=(40,50,56)
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)
img=Image.open(ART+"ui/ui_world_map.png").convert("RGBA"); WD,HT=img.size
img=ImageEnhance.Brightness(img).enhance(0.75); d=ImageDraw.Draw(img)
# highlight the picked claim (Tempest's Hold: cells (10,5),(11,5),(10,6),(11,6),(12,6))
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
for (i,j) in [(10,5),(11,5),(10,6),(11,6),(12,6)]: od.rectangle([i*64,j*64,i*64+63,j*64+63],fill=(57,255,140,55))
img.alpha_composite(ov); d=ImageDraw.Draw(img)
g=Image.new("RGBA",(WD,HT),(0,0,0,0)); gd=ImageDraw.Draw(g)
for seg in [((640,320),(768,320)),((768,320),(768,384)),((768,384),(832,384)),((832,384),(832,448)),((832,448),(640,448)),((640,448),(640,320))]:
    gd.line(seg,fill=(57,255,140,200),width=6)
img.alpha_composite(g.filter(ImageFilter.GaussianBlur(4))); d=ImageDraw.Draw(img)
def brackets(x0,y0,x1,y1,b=12,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
DX=880
d.rectangle([DX,0,WD,HT],fill=(12,16,20)); d.line([(DX,0),(DX,HT)],fill=GREEN,width=2)
gg=Image.new("RGBA",(WD,HT),(0,0,0,0)); ImageDraw.Draw(gg).rectangle([DX-6,0,DX+2,HT],fill=(57,255,140,60))
img.alpha_composite(gg.filter(ImageFilter.GaussianBlur(5))); d=ImageDraw.Draw(img)
px=DX+24; pr=WD-24
img.alpha_composite(G["chip"](40,G["claims"],True),(px,20))
d.text((px+52,18),"Tempest's Hold",font=F(20,True),fill=W)
d.text((px+52,46),"Your claim  ·  Owner  ·  Active property",font=F(12),fill=GREEN)
d.line([(px,74),(pr,74)],fill=LINE)
# your land summary
y=86; d.text((px,y),"YOUR LAND",font=F(10,True),fill=MUTE)
for k,(lab,val,fr) in enumerate([("Chunks","14 / 25",14/25),("Properties","2 / 3",2/3)]):
    cw=(pr-px-10)//2; bx=px+k*(cw+10); d.rectangle([bx,y+16,bx+cw,y+64],fill=CARD,outline=LINE)
    d.text((bx+10,y+22),lab,font=F(11),fill=MUTE); d.text((bx+cw-10-d.textlength(val,font=MONO(13)),y+21),val,font=MONO(13),fill=W)
    d.rectangle([bx+10,y+46,bx+cw-10,y+52],fill=(30,38,44)); d.rectangle([bx+10,y+46,bx+10+(cw-20)*fr,y+52],fill=GREEN)
# this claim
y=166; d.text((px,y),"THIS CLAIM",font=F(10,True),fill=MUTE)
info=[("Chunks","5"),("Center","1302, -410"),("Trusted","3 players"),("Sub-areas","2")]
for k,(lab,val) in enumerate(info):
    bx=px+(k%2)*190; by=y+16+(k//2)*36
    d.text((bx,by+4),lab,font=F(12),fill=MUTE); d.text((bx+80,by+3),val,font=F(13,True),fill=W)
y=262; d.text((px,y),"SUB-AREAS",font=F(10,True),fill=MUTE)
for k,(n,sz) in enumerate([("Farm Plot","12 × 9"),("Vault","5 × 5")]):
    ry=y+16+k*36; d.rectangle([px,ry,pr,ry+30],fill=CARD,outline=LINE)
    d.rectangle([px+10,ry+8,px+24,ry+22],outline=(120,200,255),width=2); d.text((px+34,ry+7),n,font=F(13,True),fill=W); d.text((pr-12-d.textlength(sz,font=MONO(12)),ry+8),sz,font=MONO(12),fill=MUTE)
y=354; d.text((px,y),"CLAIM MARKS",font=F(10,True),fill=MUTE); d.text((pr-d.textlength("Seen by everyone trusted here",font=F(10)),y),"Seen by everyone trusted here",font=F(10),fill=MUTE)
for k,(e,n) in enumerate([("home","Base"),("farm","Farms"),("treasure","Storage")]):
    ry=y+16+k*40; d.rectangle([px,ry,pr,ry+34],fill=CARD,outline=LINE)
    ic=Image.open(ART+f"marks/mark_{e}_32.png").convert("RGBA").resize((28,28),Image.LANCZOS); img.alpha_composite(ic,(px+4,ry+3))
    d.text((px+42,ry+8),n,font=F(13,True),fill=W)
    img.alpha_composite(A["act_chip"](24,A["edit"]),(pr-60,ry+5)); img.alpha_composite(A["act_chip"](24,A["remove"]),(pr-30,ry+5))
ry=y+16+3*40; d.rectangle([px,ry,pr,ry+30],fill=(18,30,26),outline=(30,90,60)); img.alpha_composite(A["act_chip"](22,A["newmark"]),(px+6,ry+4))
d.text((px+38,ry+7),"Add a claim mark",font=F(12,True),fill=GREEN)
# actions
ay=HT-66; d.line([(px,ay-12),(pr,ay-12)],fill=LINE)
def btn(x,w,label,fn,primary=False):
    d.rectangle([x,ay,x+w,ay+44],fill=(24,52,40) if primary else CARD,outline=GREEN if primary else LINE)
    img.alpha_composite(A["act_chip"](28,fn),(x+8,ay+8)); d.text((x+44,ay+13),label,font=F(12,True),fill=GREEN if primary else W)
btn(px,184,"Open Land Config",A["settings"],True); btn(px+194,pr-px-194,"Claim more here",A["newmark"])
img.convert("RGB").save("ui_claims_panel.png"); print("ok")
