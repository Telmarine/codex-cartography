import os, math
from PIL import Image, ImageDraw, ImageFont
SRC="/mnt/user-data/outputs/cartography-art/src/"; ART="/mnt/user-data/outputs/cartography-art/"
def load(name):
    s=open(SRC+name).read(); s=s[:s.index("\nSET")]
    cwd=os.getcwd(); os.chdir(SRC); ns={}; exec(s,ns); os.chdir(cwd); return ns
A=load("actions.py")
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236); MUTE=(130,142,150); GREY=(96,104,110); LINE=(40,50,56)
F=lambda s,b=False: ImageFont.truetype("/usr/share/fonts/opentype/inter/Inter-%s.otf"%("SemiBold" if b else "Medium"),s)
MONO=lambda s: ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",s)
img=Image.open(ART+"ui/ui_world_map.png").convert("RGBA"); WD,HT=img.size
cx,cy=860,410; ch=(13*64,6*64)
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
od.rectangle([ch[0],ch[1],ch[0]+63,ch[1]+63],fill=(57,255,140,45),outline=GREEN+(255,),width=2)
img.alpha_composite(ov); d=ImageDraw.Draw(img)
def glyph(fn,size,col=W):
    N=size*8; im=Image.new("RGBA",(N,N),(0,0,0,0)); dd=ImageDraw.Draw(im); fn(dd,N/2,N*0.04,col); return im.resize((size,size),Image.LANCZOS)
def claimg(d,c,u,f):
    m=5.6*u; d.rectangle([c-m,c-m,c+m,c+m],outline=f,width=int(1.2*u)); d.line([(c-3*u,c),(c+3*u,c)],fill=f,width=int(1.3*u)); d.line([(c,c-3*u),(c,c+3*u)],fill=f,width=int(1.3*u))
def unclaimg(d,c,u,f):
    m=5.6*u; d.rectangle([c-m,c-m,c+m,c+m],outline=f,width=int(1.2*u)); d.line([(c-3*u,c),(c+3*u,c)],fill=f,width=int(1.3*u))
def brackets(x0,y0,x1,y1,b=10,w=2,c=GREEN):
    for x,y,sx,sy in [(x0,y0,1,1),(x1,y0,-1,1),(x0,y1,1,-1),(x1,y1,-1,-1)]:
        d.line([(x,y),(x+sx*b,y)],fill=c,width=w); d.line([(x,y),(x,y+sy*b)],fill=c,width=w)
# cursor
mx,my=cx+6,cy+6
d.polygon([(cx,cy),(cx,cy+16),(cx+4,cy+12),(cx+7,cy+18),(cx+9,cy+17),(cx+6,cy+11),(cx+11,cy+11)],fill=W,outline=DARK)
rows=[("New mark here",A["newmark"],True,None),("Copy coordinates",A["copy"],True,None),("Travel here",A["travel"],True,None),
      None,("Claim this chunk",claimg,True,"14 / 25"),("Unclaim",unclaimg,False,None)]
MW=250; RH=34; x0,y0=cx+14,cy+8
h=46+sum(10 if r is None else RH for r in rows)+8
sh=Image.new("RGBA",(WD,HT),(0,0,0,0)); ImageDraw.Draw(sh).rectangle([x0+4,y0+6,x0+MW+4,y0+h+6],fill=(0,0,0,110))
img.alpha_composite(sh); d=ImageDraw.Draw(img)
d.rectangle([x0,y0,x0+MW,y0+h],fill=(12,16,20),outline=(57,255,140)); brackets(x0-3,y0-3,x0+MW+3,y0+h+3,12,2)
d.text((x0+12,y0+8),"Chunk 77, -25",font=F(12,True),fill=GREEN); d.text((x0+12,y0+26),"X 1244   Z -394",font=MONO(11),fill=MUTE)
d.line([(x0+8,y0+44),(x0+MW-8,y0+44)],fill=LINE)
y=y0+48; hov=4
for i,r in enumerate(rows):
    if r is None:
        d.line([(x0+8,y+5),(x0+MW-8,y+5)],fill=LINE); y+=10; continue
    name,fn,en,extra=r
    if i==hov: d.rectangle([x0+2,y,x0+MW-2,y+RH-2],fill=(24,52,40)); d.rectangle([x0+2,y,x0+5,y+RH-2],fill=GREEN)
    col=(GREEN if i==hov else W) if en else GREY
    img.alpha_composite(glyph(fn,32,col),(x0+8,y+1)); d=ImageDraw.Draw(img)
    d.text((x0+46,y+8),name,font=F(13,True),fill=col)
    if extra: d.text((x0+MW-14-d.textlength(extra,font=MONO(11)),y+10),extra,font=MONO(11),fill=GREEN if i==hov else MUTE)
    y+=RH
# hover reason on the greyed row
ty=y-RH+2; tx=None; t1,t2="Unclaim","This chunk isn't claimed"
tw=max(d.textlength(t1,font=F(12,True)),d.textlength(t2,font=F(11))); tx=int(x0-tw-34)
d.rectangle([tx,ty,tx+tw+20,ty+44],fill=DARK); brackets(tx,ty,tx+tw+20,ty+44,6,1,(120,128,134))
d.text((tx+10,ty+5),t1,font=F(12,True),fill=W); d.text((tx+10,ty+23),t2,font=F(11),fill=MUTE)
img.convert("RGB").save("ui_map_menu.png"); print("ok")
