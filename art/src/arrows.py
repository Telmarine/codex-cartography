from PIL import Image, ImageDraw, ImageFilter, ImageFont
GREEN=(57,255,140); WHITE=(235,240,245); DARK=(8,30,18); EDGE=(10,12,14)
def arrow(size,fill,edge,glow=None):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); c=N/2
    pts=[(c,N*0.06),(N*0.86,N*0.90),(c,N*0.68),(N*0.14,N*0.90)]   # notched chevron, points up
    if glow:
        g=Image.new("RGBA",(N,N),(0,0,0,0)); ImageDraw.Draw(g).polygon(pts,fill=glow+(170,))
        im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(N*0.07)))
    d=ImageDraw.Draw(im)
    d.polygon(pts,fill=fill)
    d.line(pts+[pts[0]],fill=edge,width=int(N*0.07),joint="curve")
    # thin center ridge for a "code" look
    d.line([(c,N*0.18),(c,N*0.60)],fill=edge+(120,),width=int(N*0.035))
    return im.resize((size,size),Image.LANCZOS)
you32=arrow(32,GREEN,DARK,GREEN); you16=arrow(16,GREEN,DARK,GREEN)
oth32=arrow(32,WHITE,EDGE); oth16=arrow(16,WHITE,EDGE)
you32.save("arrow_player_32.png"); you16.save("arrow_player_16.png"); oth32.save("arrow_other_32.png"); oth16.save("arrow_other_16.png")
sheet=Image.new("RGBA",(560,300),(8,10,12,255))
sheet.alpha_composite(you32.resize((128,128),Image.NEAREST),(20,20)); sheet.alpha_composite(oth32.resize((128,128),Image.NEAREST),(170,20))
for i,(a,b) in enumerate([(you32,you16),(oth32,oth16)]):
    sheet.alpha_composite(a,(340+i*90,40)); sheet.alpha_composite(b,(380+i*90,48))
bg=Image.new("RGBA",(240,110),(58,98,64,255)); bd=ImageDraw.Draw(bg); bd.rectangle([0,64,240,110],fill=(40,88,112,255)); bd.rectangle([150,0,240,64],fill=(104,108,112,255))
bg.alpha_composite(you32.rotate(-30,resample=Image.BICUBIC),(60,30)); bg.alpha_composite(oth32.rotate(70,resample=Image.BICUBIC),(160,50))
sheet.alpha_composite(bg,(300,150))
d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
d.text((20,156),"you (x4)",font=f,fill=(140,160,150)); d.text((170,156),"other players (x4)",font=f,fill=(140,160,150))
d.text((340,90),"32 / 16 px",font=f,fill=(140,160,150)); d.text((300,266),"on the map, turned",font=f,fill=(140,160,150))
sheet.convert("RGB").save("arrows_preview.png"); print("ok")
