package com.jams.pdfreader;
import android.graphics.*;
/** Lightweight offline automatic rectangular page-boundary estimate. Falls back to full image. */
public final class AutoCrop {
 private AutoCrop(){}
 public static Bitmap crop(Bitmap src){int w=src.getWidth(),h=src.getHeight();int sw=Math.min(w,480),sh=Math.min(h,640);Bitmap small=Bitmap.createScaledBitmap(src,sw,sh,true);int[] pix=new int[sw*sh];small.getPixels(pix,0,sw,0,0,sw,sh);int[] gray=new int[pix.length];for(int i=0;i<pix.length;i++){int c=pix[i];gray[i]=(Color.red(c)*30+Color.green(c)*59+Color.blue(c)*11)/100;}
 int[] cols=new int[sw],rows=new int[sh];for(int y=2;y<sh-2;y+=2)for(int x=2;x<sw-2;x+=2){int i=y*sw+x;int edge=Math.abs(gray[i+2]-gray[i-2])+Math.abs(gray[i+2*sw]-gray[i-2*sw]);if(edge>45){cols[x]+=edge;rows[y]+=edge;}}
 int left=0,right=sw-1,top=0,bottom=sh-1;int cmax=0,rmax=0;for(int v:cols)cmax=Math.max(cmax,v);for(int v:rows)rmax=Math.max(rmax,v);
 int cx=Math.max(1,cmax/5),ry=Math.max(1,rmax/5);for(int x=sw/20;x<sw/2;x++)if(cols[x]>cx){left=x;break;}for(int x=sw-sw/20-1;x>sw/2;x--)if(cols[x]>cx){right=x;break;}for(int y=sh/20;y<sh/2;y++)if(rows[y]>ry){top=y;break;}for(int y=sh-sh/20-1;y>sh/2;y--)if(rows[y]>ry){bottom=y;break;}
 if(right-left<sw*.45||bottom-top<sh*.45)return src;int l=Math.max(0,(int)(left*(w/(float)sw))),t=Math.max(0,(int)(top*(h/(float)sh)));int r=Math.min(w,(int)(right*(w/(float)sw))),b=Math.min(h,(int)(bottom*(h/(float)sh)));if(r<=l||b<=t)return src;return Bitmap.createBitmap(src,l,t,r-l,b-t);
 }
}
