import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont
EDGE=(10,12,14)
def dot(size, col, shape):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); d=ImageDraw.Draw(im); c=N/2; r=N*0.36
    ew=int(N*0.09)
    if shape=="hostile":   # red diamond
        p=[(c,c-r),(c+r,c),(c,c+r),(c-r,c)]; d.polygon(p,fill=col); d.line(p+[p[0]],fill=EDGE,width=ew,joint="curve")
    elif shape=="passive": # green circle
        d.ellipse([c-r,c-r,c+r,c+r],fill=EDGE); d.ellipse([c-r+ew,c-r+ew,c+r-ew,c+r-ew],fill=col)
    elif shape=="pet":     # heart
        k=r*0.95
        d.ellipse([c-k,c-k*0.75,c,c+k*0.2],fill=EDGE); d.ellipse([c,c-k*0.75,c+k,c+k*0.2],fill=EDGE)
        d.polygon([(c-k*0.98,c-k*0.15),(c+k*0.98,c-k*0.15),(c,c+k*1.05)],fill=EDGE)
        k2=k*0.72
        d.ellipse([c-k2-0.02*N,c-k2*0.75,c-0.02*N,c+k2*0.2],fill=col); d.ellipse([c+0.02*N,c-k2*0.75,c+k2+0.02*N,c+k2*0.2],fill=col)
        d.polygon([(c-k2*1.0,c-k2*0.1),(c+k2*1.0,c-k2*0.1),(c,c+k2*1.1)],fill=col)
    elif shape=="vehicle": # rounded square
        d.rounded_rectangle([c-r*0.95,c-r*0.7,c+r*0.95,c+r*0.7],radius=r*0.25,fill=EDGE)
        d.rounded_rectangle([c-r*0.95+ew,c-r*0.7+ew,c+r*0.95-ew,c+r*0.7-ew],radius=r*0.15,fill=col)
    elif shape=="item":    # 4-point sparkle
        rr=r*1.05; w=r*0.32
        p=[(c,c-rr),(c+w,c-w),(c+rr,c),(c+w,c+w),(c,c+rr),(c-w,c+w),(c-rr,c),(c-w,c-w)]
        d.polygon(p,fill=col); d.line(p+[p[0]],fill=EDGE,width=int(ew*0.7),joint="curve")
    return im.resize((size,size),Image.LANCZOS)
SET=[("hostile",(255,80,80)),("passive",(120,230,140)),("pet",(255,140,200)),("vehicle",(255,190,80)),("item",(140,200,255))]
for n,col in SET:
    dot(16,col,n).save(f"sweep_{n}_16.png"); dot(12,col,n).save(f"sweep_{n}_12.png")
sheet=Image.new("RGBA",(620,250),(8,10,12,255)); d=ImageDraw.Draw(sheet)
f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
labels={"hostile":"hostile mobs","passive":"passive mobs","pet":"pets / tamed","vehicle":"boats / carts","item":"items"}
for i,(n,col) in enumerate(SET):
    x=20+i*120
    sheet.alpha_composite(dot(16,col,n).resize((96,96),Image.NEAREST),(x,14))
    sheet.alpha_composite(Image.open(f"sweep_{n}_16.png"),(x,120)); sheet.alpha_composite(Image.open(f"sweep_{n}_12.png"),(x+30,122))
    d.text((x,144),labels[n],font=f,fill=(140,160,150))
bg=Image.new("RGBA",(580,60),(58,98,64,255)); bd=ImageDraw.Draw(bg); bd.rectangle([300,0,580,60],fill=(40,88,112,255))
for i,(n,col) in enumerate(SET*2):
    bg.alpha_composite(Image.open(f"sweep_{n}_16.png"),(20+i*55,12+(i%3)*10))
sheet.alpha_composite(bg,(20,172)); d.text((20,236),"on the map (16 px)",font=f,fill=(140,160,150))
sheet.convert("RGB").save("sweep_preview.png"); print("ok")
