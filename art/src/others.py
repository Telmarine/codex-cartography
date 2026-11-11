import math
from PIL import Image, ImageDraw, ImageFont
exec(open('arrows.py').read().split("you32=")[0])
WHITE=(235,240,245); EDGE=(10,12,14)
def other(size):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); d=ImageDraw.Draw(im); c=N/2
    r=N*0.24; cy=c+N*0.08
    # heading tab: small wedge above the dot, pointing the way they face
    d.polygon([(c,N*0.06),(c+N*0.15,cy-r*0.55),(c-N*0.15,cy-r*0.55)],fill=WHITE,outline=EDGE)
    d.line([(c,N*0.06),(c+N*0.15,cy-r*0.55),(c-N*0.15,cy-r*0.55),(c,N*0.06)],fill=EDGE,width=int(N*0.05),joint="curve")
    # ring dot: white ring, dark core
    d.ellipse([c-r,cy-r,c+r,cy+r],fill=EDGE)
    d.ellipse([c-r+N*0.04,cy-r+N*0.04,c+r-N*0.04,cy+r-N*0.04],fill=WHITE)
    d.ellipse([c-r*0.42,cy-r*0.42,c+r*0.42,cy+r*0.42],fill=EDGE)
    return im.resize((size,size),Image.LANCZOS)
oth32=other(32); oth16=other(16); oth32.save("arrow_other_32.png"); oth16.save("arrow_other_16.png")
you32=Image.open("arrow_player_32.png"); you16=Image.open("arrow_player_16.png")
sheet=Image.new("RGBA",(560,300),(8,10,12,255))
sheet.alpha_composite(you32.resize((128,128),Image.NEAREST),(20,20)); sheet.alpha_composite(oth32.resize((128,128),Image.NEAREST),(170,20))
for i,(a,b) in enumerate([(you32,you16),(oth32,oth16)]):
    sheet.alpha_composite(a,(340+i*90,40)); sheet.alpha_composite(b,(380+i*90,48))
bg=Image.new("RGBA",(240,110),(58,98,64,255)); bd=ImageDraw.Draw(bg); bd.rectangle([0,64,240,110],fill=(40,88,112,255)); bd.rectangle([150,0,240,64],fill=(104,108,112,255))
bg.alpha_composite(you32.rotate(-30,resample=Image.BICUBIC),(60,30)); bg.alpha_composite(oth32.rotate(70,resample=Image.BICUBIC),(160,50)); bg.alpha_composite(oth32.rotate(-150,resample=Image.BICUBIC),(110,60))
sheet.alpha_composite(bg,(300,150))
d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
d.text((20,156),"you (x4)",font=f,fill=(140,160,150)); d.text((170,156),"other players (x4)",font=f,fill=(140,160,150))
d.text((340,90),"32 / 16 px",font=f,fill=(140,160,150)); d.text((300,266),"on the map, turned",font=f,fill=(140,160,150))
sheet.convert("RGB").save("arrows_preview.png"); print("ok")
