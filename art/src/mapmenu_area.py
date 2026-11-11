import os
from PIL import Image, ImageDraw, ImageFont
exec(open("mapmenu.py").read())
img=Image.open(ART+"ui/ui_world_map.png").convert("RGBA"); WD,HT=img.size
c0,r0,c1,r1=13,3,15,5   # chunk columns/rows selected (3 x 3)
x0s,y0s,x1s,y1s=c0*64,r0*64,(c1+1)*64,(r1+1)*64
ov=Image.new("RGBA",(WD,HT),(0,0,0,0)); od=ImageDraw.Draw(ov)
od.rectangle([x0s,y0s,x1s,y1s],fill=(57,255,140,40))
for x in range(x0s,x1s,64): od.line([(x,y0s),(x,y1s)],fill=(57,255,140,90))
for y in range(y0s,y1s,64): od.line([(x0s,y),(x1s,y)],fill=(57,255,140,90))
for k in range(x0s,x1s,10):
    od.line([(k,y0s),(k+5,y0s)],fill=GREEN+(255,),width=2); od.line([(k,y1s),(k+5,y1s)],fill=GREEN+(255,),width=2)
for k in range(y0s,y1s,10):
    od.line([(x0s,k),(x0s,k+5)],fill=GREEN+(255,),width=2); od.line([(x1s,k),(x1s,k+5)],fill=GREEN+(255,),width=2)
img.alpha_composite(ov); d=ImageDraw.Draw(img)
lab="3 × 3  ·  9 chunks"; f=F(12,True); tw=d.textlength(lab,font=f)
d.rectangle([x0s,y0s-26,x0s+tw+16,y0s-4],fill=DARK,outline=GREEN); d.text((x0s+8,y0s-23),lab,font=f,fill=GREEN)
cx,cy=x1s,y1s
d.polygon([(cx,cy),(cx,cy+16),(cx+4,cy+12),(cx+7,cy+18),(cx+9,cy+17),(cx+6,cy+11),(cx+11,cy+11)],fill=W,outline=DARK)
def printg(d,c,u,f): A["printer"](d,c,u,f)
rows=[("Claim these chunks",claimg,True,"uses 9 of 11"),("Unclaim your chunks here",unclaimg,False,None),None,
      ("Print this area",printg,True,None),("Copy corner coordinates",A["copy"],True,None)]
MW=280; RH=34; mx0,my0=cx-MW-14,cy+10
h=46+sum(10 if r is None else RH for r in rows)+8
sh=Image.new("RGBA",(WD,HT),(0,0,0,0)); ImageDraw.Draw(sh).rectangle([mx0+4,my0+6,mx0+MW+4,my0+h+6],fill=(0,0,0,110))
img.alpha_composite(sh); d=ImageDraw.Draw(img)
d.rectangle([mx0,my0,mx0+MW,my0+h],fill=(12,16,20),outline=GREEN); brackets(mx0-3,my0-3,mx0+MW+3,my0+h+3,12,2)
d.text((mx0+12,my0+8),"Area  ·  9 chunks",font=F(12,True),fill=GREEN); d.text((mx0+12,my0+26),"X 832 → 1023   Z 192 → 383",font=MONO(11),fill=MUTE)
d.line([(mx0+8,my0+44),(mx0+MW-8,my0+44)],fill=LINE)
y=my0+48; hov=0
for i,r in enumerate(rows):
    if r is None: d.line([(mx0+8,y+5),(mx0+MW-8,y+5)],fill=LINE); y+=10; continue
    name,fn,en,extra=r
    if i==hov: d.rectangle([mx0+2,y,mx0+MW-2,y+RH-2],fill=(24,52,40)); d.rectangle([mx0+2,y,mx0+5,y+RH-2],fill=GREEN)
    col=(GREEN if i==hov else W) if en else GREY
    img.alpha_composite(glyph(fn,32,col),(mx0+8,y+1)); d=ImageDraw.Draw(img)
    d.text((mx0+46,y+8),name,font=F(13,True),fill=col)
    if extra: d.text((mx0+MW-14-d.textlength(extra,font=MONO(11)),y+10),extra,font=MONO(11),fill=GREEN if i==hov else MUTE)
    y+=RH
ty=my0+48+RH+2; t1,t2="Unclaim your chunks here","None of these chunks are yours"
tw=max(d.textlength(t1,font=F(12,True)),d.textlength(t2,font=F(11))); tx=int(mx0-tw-34)
d.rectangle([tx,ty,tx+tw+20,ty+44],fill=DARK); brackets(tx,ty,tx+tw+20,ty+44,6,1,(120,128,134))
d.text((tx+10,ty+5),t1,font=F(12,True),fill=W); d.text((tx+10,ty+23),t2,font=F(11),fill=MUTE)
img.convert("RGB").save("ui_map_menu_area.png"); print("ok")
