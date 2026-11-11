import math
from PIL import Image, ImageDraw, ImageFont
exec(open('glyphs.py').read().split("def claims")[0])
RED=(255,80,80)
def act_chip(size, draw, col=GREEN):
    S=8; N=size*S; im=Image.new("RGBA",(N,N),(0,0,0,0)); d=ImageDraw.Draw(im); c=N/2; m=N*0.06
    d.rectangle([m,m,N-m,N-m],fill=DARK+(240,)); d.rectangle([m,m,N-m,N-m],outline=col,width=int(N*0.045))
    draw(d,c,N*0.04,col if col==RED else W)
    return im.resize((size,size),Image.LANCZOS)
def travel(d,c,u,f):   # bold arrow flying into a target ring
    d.ellipse([c+0.4*u,c-3.0*u,c+5.8*u,c+2.4*u],outline=f,width=int(1.0*u))
    d.ellipse([c+2.3*u,c-1.1*u,c+3.9*u,c+0.5*u],fill=f)
    d.line([(c-5.6*u,c-0.3*u),(c-0.6*u,c-0.3*u)],fill=f,width=int(1.5*u))
    d.polygon([(c-1.6*u,c-3.0*u),(c+1.6*u,c-0.3*u),(c-1.6*u,c+2.4*u)],fill=f)
def share(d,c,u,f):    # three linked nodes
    pts=[(c+3.6*u,c-3.8*u),(c-3.6*u,c),(c+3.6*u,c+3.8*u)]
    d.line([pts[0],pts[1],pts[2]],fill=f,width=int(0.9*u))
    for x,y in pts: d.ellipse([x-1.7*u,y-1.7*u,x+1.7*u,y+1.7*u],fill=f)
def edit(d,c,u,f):     # stylus
    a=math.radians(45); L=5.2*u; w=1.3*u
    ca,sa=math.cos(a),math.sin(a)
    def T(x,y): return (c+x*ca-y*sa, c+x*sa+y*ca)
    d.polygon([T(-L,-w),T(L-1.8*u,-w),T(L,0),T(L-1.8*u,w),T(-L,w)],fill=f)
    d.line([T(-L+1.4*u,-w),T(-L+1.4*u,w)],fill=DARK,width=int(0.5*u))
def hide(d,c,u,f):     # eye with slash
    d.polygon([(c-5.4*u,c),(c-2.2*u,c-3.0*u),(c+2.2*u,c-3.0*u),(c+5.4*u,c),(c+2.2*u,c+3.0*u),(c-2.2*u,c+3.0*u)],outline=f,width=int(0.9*u))
    d.ellipse([c-1.6*u,c-1.6*u,c+1.6*u,c+1.6*u],fill=f)
    d.line([(c-4.8*u,c+4.8*u),(c+4.8*u,c-4.8*u)],fill=DARK,width=int(2.0*u)); d.line([(c-4.8*u,c+4.8*u),(c+4.8*u,c-4.8*u)],fill=f,width=int(0.9*u))
def remove(d,c,u,f):   # bin
    d.rectangle([c-4.2*u,c-4.6*u,c+4.2*u,c-3.4*u],fill=f); d.rectangle([c-1.4*u,c-5.6*u,c+1.4*u,c-4.6*u],fill=f)
    d.polygon([(c-3.4*u,c-2.6*u),(c+3.4*u,c-2.6*u),(c+2.8*u,c+5.4*u),(c-2.8*u,c+5.4*u)],fill=f)
    for x in (-1.4,0,1.4): d.line([(c+x*u,c-1.4*u),(c+x*u,c+4.0*u)],fill=DARK,width=int(0.55*u))
def confirm(d,c,u,f):
    d.line([(c-4.6*u,c+0.2*u),(c-1.2*u,c+3.6*u),(c+4.8*u,c-3.6*u)],fill=f,width=int(1.4*u),joint="curve")
def copy(d,c,u,f):     # two stacked cards
    d.rectangle([c-4.4*u,c-4.4*u,c+1.6*u,c+1.6*u],outline=f,width=int(0.9*u))
    d.rectangle([c-1.6*u,c-1.6*u,c+4.4*u,c+4.4*u],fill=DARK,outline=f,width=int(0.9*u))
def newmark(d,c,u,f):  # diamond with plus
    r=5*u; d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],outline=f,width=int(0.9*u))
    d.line([(c-2.2*u,c),(c+2.2*u,c)],fill=f,width=int(1.0*u)); d.line([(c,c-2.2*u),(c,c+2.2*u)],fill=f,width=int(1.0*u))
def search(d,c,u,f):
    d.ellipse([c-4.6*u,c-4.6*u,c+1.6*u,c+1.6*u],outline=f,width=int(1.0*u))
    d.line([(c+1.0*u,c+1.0*u),(c+4.8*u,c+4.8*u)],fill=f,width=int(1.4*u))
def lens(d,c,u,f):     # lens dial: circle split in quarters
    d.ellipse([c-5*u,c-5*u,c+5*u,c+5*u],outline=f,width=int(0.9*u))
    d.pieslice([c-5*u,c-5*u,c+5*u,c+5*u],start=-90,end=0,fill=f)
    d.line([(c-5*u,c),(c+5*u,c)],fill=f,width=int(0.5*u)); d.line([(c,c-5*u),(c,c+5*u)],fill=f,width=int(0.5*u))
def settings(d,c,u,f): # sliders
    for i,(y,k) in enumerate([(-3.4,-1.6),(0,2.0),(3.4,-0.4)]):
        d.line([(c-5*u,c+y*u),(c+5*u,c+y*u)],fill=f,width=int(0.6*u))
        d.rectangle([c+k*u-1.0*u,c+y*u-1.3*u,c+k*u+1.0*u,c+y*u+1.3*u],fill=f)
def printer(d,c,u,f):  # print: framed picture with mountains
    d.rectangle([c-5*u,c-4*u,c+5*u,c+4*u],outline=f,width=int(0.9*u))
    d.polygon([(c-4*u,c+3.2*u),(c-1*u,c-0.8*u),(c+1*u,c+1.2*u),(c+2.4*u,c-0.2*u),(c+4.2*u,c+3.2*u)],fill=f)
    d.ellipse([c+1.6*u,c-3.2*u,c+3.2*u,c-1.6*u],fill=f)
SET=[("travel",travel,GREEN),("share",share,GREEN),("edit",edit,GREEN),("hide",hide,GREEN),("remove",remove,GREEN),("remove_confirm",remove,RED),
     ("confirm",confirm,GREEN),("copy",copy,GREEN),("new_mark",newmark,GREEN),("search",search,GREEN),("lens",lens,GREEN),("settings",settings,GREEN),("print",printer,GREEN)]
for n,f,col in SET: act_chip(24,f,col).save(f"action_{n}_24.png")
cols=7; sheet=Image.new("RGBA",(cols*120+20,2*150+40),(8,10,12,255)); d=ImageDraw.Draw(sheet)
F=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
for i,(n,f,col) in enumerate(SET):
    x=20+(i%cols)*120; y=14+(i//cols)*150
    sheet.alpha_composite(act_chip(24,f,col).resize((96,96),Image.NEAREST),(x,y))
    sheet.alpha_composite(Image.open(f"action_{n}_24.png"),(x,y+102))
    d.text((x+32,y+108),n.replace('_',' ').replace('remove confirm','delete?'),font=F,fill=(140,160,150))
sheet.convert("RGB").save("actions_preview.png"); print("ok")
