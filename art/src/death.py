from PIL import Image, ImageDraw, ImageFont
RED=(255,60,60)
def death(size):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); d=ImageDraw.Draw(im)
    c=N/2; r=N*0.44
    d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],fill=(0,0,0,255))
    d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],outline=(70,74,78),width=max(S,int(N*0.02)))   # thin edge so it reads on dark ground
    u=N*0.033; k=4.4*u; w=int(2.0*u)
    d.line([(c-k,c-k),(c+k,c+k)],fill=RED,width=w); d.line([(c-k,c+k),(c+k,c-k)],fill=RED,width=w)
    return im.resize((size,size),Image.LANCZOS)
death(64).save("mark_death_64.png"); death(32).save("mark_death_32.png")
sheet=Image.new("RGBA",(560,300),(8,10,12,255))
b=death(64); sheet.alpha_composite(b.resize((256,256),Image.NEAREST),(20,22))
sheet.alpha_composite(b,(320,60)); sheet.alpha_composite(death(32),(420,76))
bg=Image.new("RGBA",(200,110),(58,98,64,255)); bd=ImageDraw.Draw(bg); bd.rectangle([0,60,200,110],fill=(40,88,112,255))
bg.alpha_composite(b,(68,23)); sheet.alpha_composite(bg,(320,160))
d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
d.text((24,282),"x4 preview",font=f,fill=(140,160,150)); d.text((320,128),"64px",font=f,fill=(140,160,150)); d.text((420,128),"32px",font=f,fill=(140,160,150)); d.text((320,276),"on the map",font=f,fill=(140,160,150))
sheet.convert("RGB").save("mark_death_preview.png"); print("ok")
