from PIL import Image, ImageDraw, ImageFilter, ImageFont
GREEN=(57,255,140); DARK=(14,18,22); W=(232,240,236)
def badge(size, glyph, col=GREEN):
    S=8; N=size*S
    im=Image.new("RGBA",(N,N),(0,0,0,0))
    g=Image.new("RGBA",(N,N),(0,0,0,0)); gd=ImageDraw.Draw(g)
    c=N/2; r=N*0.44
    gd.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],fill=col+(150,))
    im=Image.alpha_composite(im,g.filter(ImageFilter.GaussianBlur(N*0.04))); d=ImageDraw.Draw(im)
    r=N*0.40
    d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],fill=DARK+(245,))
    d.polygon([(c,c-r),(c+r,c),(c,c+r),(c-r,c)],outline=col,width=max(int(N*0.045),S))
    r2=r*0.82
    d.polygon([(c,c-r2),(c+r2,c),(c,c+r2),(c-r2,c)],outline=col+(70,),width=max(S,int(N*0.012)))
    glyph(d,c,N*0.033)
    return im.resize((size,size),Image.LANCZOS)
def keep(d,c,u):
    bx0,bx1=c-4.6*u,c+4.6*u; top=c-1.6*u; bot=c+5.4*u
    d.rectangle([bx0,top,bx1,bot],fill=W)
    mw=2.2*u; gap=(bx1-bx0-3*mw)/2
    for i in range(3):
        x=bx0+i*(mw+gap); d.rectangle([x,top-2.6*u,x+mw,top+0.2*u],fill=W)
    d.rectangle([c-1.5*u,bot-2.8*u,c+1.5*u,bot],fill=DARK)
    d.ellipse([c-1.5*u,bot-4.3*u,c+1.5*u,bot-1.3*u],fill=DARK)
def mine(d,c,u):
    import math
    dx,dy=-0.9*u,0.9*u   # nudge toward the center
    X=lambda x:x+dx; Y=lambda y:y+dy
    d.line([(X(c-4.4*u),Y(c+4.4*u)),(X(c+2.2*u),Y(c-2.2*u))],fill=W,width=int(1.5*u))
    d.ellipse([X(c-5.1*u),Y(c+3.7*u),X(c-3.7*u),Y(c+5.1*u)],fill=W)
    cx,cy,R=X(c-1.4*u),Y(c+1.4*u),5.6*u
    po=[];pi=[]
    for i in range(41):
        t=i/40; ang=math.radians(-45-58+116*t)
        w=(1.0-abs(t-0.5)*2)**0.7*1.15*u+0.12*u
        po.append((cx+(R+w)*math.cos(ang),cy+(R+w)*math.sin(ang)))
        pi.append((cx+(R-w)*math.cos(ang),cy+(R-w)*math.sin(ang)))
    d.polygon(po+pi[::-1],fill=W)
def portal(d,c,u):
    import math
    x0,x1,y0,y1=c-3.9*u,c+3.9*u,c-5.7*u,c+5.7*u
    t=1.0*u
    d.rounded_rectangle([x0,y0,x1,y1],radius=1.2*u,fill=W)
    d.rounded_rectangle([x0+t,y0+t,x1-t,y1-t],radius=0.6*u,fill=DARK)
    pts=[]
    turns=2.0*math.pi*1.3
    for i in range(160):
        a=i/159*turns; r=0.5*u+a/turns*1.75*u
        pts.append((c+r*math.cos(a-math.pi/2), c+r*math.sin(a-math.pi/2)*1.6))
    d.line(pts,fill=W,width=int(0.95*u),joint="curve")
def treasure(d,c,u):
    # chest: domed lid + body, dark seam, latch
    x0,x1=c-4.4*u,c+4.4*u
    lt,lb=c-3.8*u,c-0.6*u
    d.rounded_rectangle([x0,lt,x1,lb+0.8*u],radius=2.2*u,fill=W)
    d.rectangle([x0,c-0.6*u,x1,c+4.0*u],fill=W)
    d.rectangle([x0,c-0.9*u,x1,c-0.1*u],fill=DARK)          # seam
    d.rectangle([c-1.1*u,c-1.9*u,c+1.1*u,c+1.3*u],fill=DARK)  # latch plate
    d.rectangle([c-0.45*u,c-1.2*u,c+0.45*u,c+0.6*u],fill=W)   # latch
    d.rectangle([x0+1.4*u,lt+0.9*u,x0+2.1*u,c+4.0*u],fill=DARK) # straps
    d.rectangle([x1-2.1*u,lt+0.9*u,x1-1.6*u,c+4.0*u],fill=DARK)
def farm(d,c,u):
    import math
    def leaf(cx,cy,ang,L,Wd):
        a=math.radians(ang); ca,sa=math.cos(a),math.sin(a)
        pts=[]
        for t in [i/12 for i in range(13)]:
            w=math.sin(t*math.pi)*Wd
            pts.append((t*L,w))
        pts+= [(t*L,-math.sin(t*math.pi)*Wd) for t in [i/12 for i in range(12,-1,-1)]]
        d.polygon([(cx+x*ca-y*sa, cy+x*sa+y*ca) for x,y in pts],fill=W)
    d.line([(c,c+6.0*u),(c,c-2.0*u)],fill=W,width=int(0.9*u))
    for i in range(3):
        y=c-2.4*u+i*2.6*u
        leaf(c+0.3*u,y,-48,4.0*u,1.15*u)
        leaf(c-0.3*u,y,-132,4.0*u,1.15*u)
    leaf(c,c-2.4*u,-90,4.0*u,1.1*u)
def shrine(d,c,u):
    # small temple: pointed roof, two pillars, step base
    d.polygon([(c,c-5.6*u),(c+5.4*u,c-1.8*u),(c-5.4*u,c-1.8*u)],fill=W)      # roof
    d.polygon([(c,c-3.9*u),(c+2.6*u,c-2.3*u),(c-2.6*u,c-2.3*u)],fill=DARK)   # gable cut
    d.rectangle([c-4.2*u,c-1.4*u,c+4.2*u,c-0.5*u],fill=W)                    # lintel
    for x in (-3.2,-1.0,1.0,3.2):
        d.rectangle([c+x*u-0.55*u,c-0.5*u,c+x*u+0.55*u,c+3.6*u],fill=W)      # pillars
    d.rectangle([c-4.6*u,c+3.8*u,c+4.6*u,c+4.7*u],fill=W)                    # step
    d.rectangle([c-5.2*u,c+5.0*u,c+5.2*u,c+5.8*u],fill=W)                    # base
def danger(d,c,u):
    import math
    # equilateral triangle placed so all three corners sit the same distance from the diamond's edges
    # (scaled about the diamond's center from the triangle that fits the diamond exactly)
    Y=(255,214,60)
    Rt=8.0*u                      # size of the triangle's fitted diamond
    top=c-Rt; base=c+(2-math.sqrt(3))*Rt; half=(math.sqrt(3)-1)*Rt
    dy=((top+base)/2-c)/-2       # halfway between the equal-margin spot and level center
    top+=dy; base+=dy
    d.polygon([(c,top),(c+half,base),(c-half,base)],fill=Y)
    m=(top+2*base)/3              # centroid
    d.polygon([(c-0.7*u,m-2.3*u),(c+0.7*u,m-2.3*u),(c+0.42*u,m+0.6*u),(c-0.42*u,m+0.6*u)],fill=DARK)
    d.ellipse([c-0.68*u,m+1.25*u,c+0.68*u,m+2.6*u],fill=DARK)
def home(d,c,u):
    # house: roof, body, door, chimney
    d.rectangle([c+2.0*u,c-5.0*u,c+3.4*u,c-1.8*u],fill=W)                    # chimney
    d.polygon([(c,c-5.4*u),(c+5.4*u,c-0.4*u),(c-5.4*u,c-0.4*u)],fill=W)      # roof
    d.rectangle([c-4.0*u,c-0.8*u,c+4.0*u,c+5.2*u],fill=W)                    # body
    d.rectangle([c-1.3*u,c+1.6*u,c+1.3*u,c+5.2*u],fill=DARK)                 # door
    d.rectangle([c+2.0*u,c+0.6*u,c+3.2*u,c+1.8*u],fill=DARK)                 # window
def village(d,c,u):
    # two small houses side by side, the right one taller
    def house(x,y,w,hgt,roof):
        d.polygon([(x,y-hgt-roof),(x+w/2+0.6*u,y-hgt),(x-w/2-0.6*u,y-hgt)],fill=W)
        d.rectangle([x-w/2,y-hgt-0.2*u,x+w/2,y],fill=W)
        d.rectangle([x-0.7*u,y-2.2*u,x+0.7*u,y],fill=DARK)
    house(c-2.7*u,c+4.0*u,4.6*u,4.2*u,2.6*u)
    house(c+2.9*u,c+4.0*u,4.4*u,6.0*u,2.6*u)
    d.line([(c-0.5*u,c+4.0*u),(c+0.6*u,c+4.0*u)],fill=W,width=int(0.6*u))
def ruin(d,c,u):
    import math
    # broken column: base, fluted shaft with a jagged snapped top, rubble beside it
    ox=-1.0*u
    d.rectangle([c+ox-3.4*u,c+3.8*u,c+ox+3.4*u,c+5.2*u],fill=W)          # base slab
    d.rectangle([c+ox-2.7*u,c+2.8*u,c+ox+2.7*u,c+3.8*u],fill=W)          # plinth
    sx0,sx1=c+ox-2.0*u,c+ox+2.0*u
    d.polygon([(sx0,c+2.8*u),(sx0,c-3.0*u),(sx0+1.0*u,c-4.6*u),(sx0+1.9*u,c-2.6*u),
               (sx0+2.8*u,c-5.4*u),(sx1,c-3.6*u),(sx1,c+2.8*u)],fill=W)   # shaft with snapped top
    for fx in (-0.7,0.7):
        d.line([(c+ox+fx*u,c-2.4*u),(c+ox+fx*u,c+2.4*u)],fill=DARK,width=int(0.45*u))   # flutes
    # rubble
    def rock(cx,cy,h,deg):
        a=math.radians(deg); pts=[(-h,-h*0.8),(h,-h),(h*1.1,h*0.7),(-h*0.9,h)]
        d.polygon([(cx+x*math.cos(a)-y*math.sin(a),cy+x*math.sin(a)+y*math.cos(a)) for x,y in pts],fill=W)
    rock(c+3.9*u,c+4.1*u,1.15*u,18)
    rock(c+3.4*u,c+1.6*u,0.75*u,-25)
def skull(d,c,u):
    # dungeon/boss: skull with eye sockets, nose, teeth
    d.ellipse([c-4.6*u,c-5.4*u,c+4.6*u,c+2.6*u],fill=W)
    d.rounded_rectangle([c-3.0*u,c+0.8*u,c+3.0*u,c+4.8*u],radius=0.8*u,fill=W)
    d.ellipse([c-3.4*u,c-2.6*u,c-0.6*u,c+0.4*u],fill=DARK)
    d.ellipse([c+0.6*u,c-2.6*u,c+3.4*u,c+0.4*u],fill=DARK)
    d.polygon([(c,c+0.7*u),(c+0.7*u,c+2.0*u),(c-0.7*u,c+2.0*u)],fill=DARK)
    for x in (-1.5,0,1.5):
        d.rectangle([c+x*u-0.25*u,c+3.0*u,c+x*u+0.25*u,c+4.8*u],fill=DARK)
def gate(d,c,u):
    d.rectangle([c-5.0*u,c-1.6*u,c+5.0*u,c+5.4*u],fill=W)
    d.ellipse([c-5.0*u,c-6.2*u,c+5.0*u,c+2.8*u],fill=W)
    d.rectangle([c-2.6*u,c-0.6*u,c+2.6*u,c+5.4*u],fill=DARK)
    d.ellipse([c-2.6*u,c-3.2*u,c+2.6*u,c+1.6*u],fill=DARK)
def flag(d,c,u):
    # meeting point: pole with a waving flag
    d.rectangle([c-3.6*u,c-5.6*u,c-2.6*u,c+5.6*u],fill=W)
    d.ellipse([c-4.0*u,c-6.4*u,c-2.2*u,c-4.6*u],fill=W)
    import math
    top=[];bot=[]
    for i in range(21):
        t=i/20; x=c-2.6*u+t*7.2*u; w=math.sin(t*math.pi*1.6)*0.9*u
        top.append((x,c-4.8*u+w)); bot.append((x,c+0.2*u+w))
    d.polygon(top+bot[::-1],fill=W)
def star(d,c,u):
    import math
    pts=[]
    for i in range(10):
        r=6.0*u if i%2==0 else 2.6*u
        a=math.radians(-90+i*36)
        pts.append((c+r*math.cos(a),c+0.6*u+r*math.sin(a)))
    d.polygon(pts,fill=W)
def question(d,c,u):
    d.arc([c-3.2*u,c-6.2*u,c+3.2*u,c+0.2*u],start=170,end=45,fill=W,width=int(1.7*u))
    d.line([(c+2.2*u,c-0.8*u),(c+0.5*u,c+0.6*u),(c,c+1.4*u),(c,c+2.4*u)],fill=W,width=int(1.7*u),joint="curve")
    d.ellipse([c-1.1*u,c+3.6*u,c+1.1*u,c+5.8*u],fill=W)
def ancient_city(d,c,u):
    # the great frame of an Ancient City: wide stepped frame with long swept horns over a dark gateway
    d.polygon([(c-5.0*u,c+4.2*u),(c-5.0*u,c-1.0*u),(c-6.4*u,c-6.0*u),(c-3.6*u,c-2.8*u),(c+3.6*u,c-2.8*u),
               (c+6.4*u,c-6.0*u),(c+5.0*u,c-1.0*u),(c+5.0*u,c+4.2*u)],fill=W)
    d.rectangle([c-3.0*u,c-1.2*u,c+3.0*u,c+4.2*u],fill=DARK)
    d.rectangle([c-1.9*u,c-0.1*u,c+1.9*u,c+4.2*u],fill=(40,140,150))   # faint sculk glow inside
    d.rectangle([c-1.1*u,c+0.7*u,c+1.1*u,c+4.2*u],fill=DARK)
    d.rectangle([c-6.0*u,c+4.2*u,c+6.0*u,c+5.4*u],fill=W)            # base
def ocean_monument(d,c,u):
    # ocean monument: stepped temple with waves below
    steps=[(5.4,1.6),(4.0,-0.6),(2.6,-2.8),(1.2,-4.8)]
    for half,top in steps:
        d.rectangle([c-half*u,c+top*u,c+half*u,c+2.6*u],fill=W)
    d.rectangle([c-0.9*u,c+0.2*u,c+0.9*u,c+2.6*u],fill=DARK)
    import math
    for row,y in enumerate((c+3.8*u,c+5.4*u)):
        pts=[]
        for i in range(31):
            t=i/30; x=c-5.4*u+t*10.8*u
            pts.append((x,y+math.sin(t*math.pi*4+row)*0.45*u))
        d.line(pts,fill=W,width=int(0.7*u),joint="curve")
if __name__=="__main__":
    import sys
    name=sys.argv[1]; g={'keep':keep,'mine':mine,'portal':portal,'treasure':treasure,'farm':farm,'shrine':shrine,'danger':danger,'home':home,'village':village,'ruin':ruin,'skull':skull,'gate':gate,'flag':flag,'star':star,'question':question,'ancient_city':ancient_city,'ocean_monument':ocean_monument}[name]
    b64=badge(64,g); b32=badge(32,g); b64.save(f"mark_{name}_64.png"); b32.save(f"mark_{name}_32.png")
    sheet=Image.new("RGBA",(560,300),(8,10,12,255))
    sheet.alpha_composite(b64.resize((256,256),Image.NEAREST),(20,22))
    sheet.alpha_composite(b64,(320,60)); sheet.alpha_composite(b32,(420,76))
    for i,col in enumerate([(120,200,255),(255,176,60),(255,90,90)]): sheet.alpha_composite(badge(64,g,col),(300+i*80,170))
    d=ImageDraw.Draw(sheet); f=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf",12)
    d.text((24,282),"x4 preview",font=f,fill=(140,160,150)); d.text((320,128),"64px",font=f,fill=(140,160,150)); d.text((420,128),"32px",font=f,fill=(140,160,150))
    sheet.convert("RGB").save(f"mark_{name}_preview.png"); print("ok")
