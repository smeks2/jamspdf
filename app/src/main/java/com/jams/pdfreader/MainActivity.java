package com.jams.pdfreader;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.*;
import android.graphics.pdf.*;
import android.net.Uri;
import android.print.*;
import android.print.pdf.PrintedPdfDocument;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 static final int OPEN=1,CAMERA=2,SAVE=3,EXPORT=4;
 Uri pdfUri, cameraUri; PdfRenderer renderer; ParcelFileDescriptor descriptor;
 int page=0; float zoom=1f;
 boolean annotating=false; int inkColor=Color.RED; float inkWidth=3f; boolean marker=false;
 final HashMap<Integer,ArrayList<InkStroke>> ink=new HashMap<>();
 final HashMap<Integer,ArrayList<InkStroke>> redoInk=new HashMap<>();
 LinearLayout annotationBar;
 static class InkStroke { Path path=new Path(); int color; float width; boolean highlight; InkStroke(int c,float w,boolean h){color=c;width=w;highlight=h;} }
 void drawInk(Canvas canvas,int index,float w,float h){ArrayList<InkStroke> strokes=ink.get(index);if(strokes==null)return;canvas.save();canvas.scale(w,h);for(InkStroke stroke:strokes){Paint paint=new Paint(3);paint.setStyle(Paint.Style.STROKE);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(stroke.width/Math.max(w,h));paint.setColor(stroke.color);if(stroke.highlight)paint.setAlpha(100);canvas.drawPath(stroke.path,paint);}canvas.restore();}
 class AnnotatedPage extends View {Bitmap bitmap;int index;InkStroke current;
  AnnotatedPage(Bitmap b,int i){super(MainActivity.this);bitmap=b;index=i;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
  @Override protected void onMeasure(int w,int h){int width=MeasureSpec.getSize(w);setMeasuredDimension(width,Math.max(1,(int)(width*(double)bitmap.getHeight()/bitmap.getWidth())));}
  @Override protected void onDraw(Canvas c){super.onDraw(c);c.drawColor(Color.WHITE);c.drawBitmap(bitmap,null,new Rect(0,0,getWidth(),getHeight()),new Paint(3));drawInk(c,index,getWidth(),getHeight());if(current!=null){Paint p=new Paint(3);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(current.width);p.setColor(current.color);if(current.highlight)p.setAlpha(100);c.save();c.scale(getWidth(),getHeight());p.setStrokeWidth(current.width/Math.max(getWidth(),getHeight()));c.drawPath(current.path,p);c.restore();}}
  @Override public boolean onTouchEvent(MotionEvent e){if(!annotating)return false;float x=Math.max(0,Math.min(1,e.getX()/Math.max(1,getWidth()))),y=Math.max(0,Math.min(1,e.getY()/Math.max(1,getHeight())));switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:current=new InkStroke(inkColor,inkWidth,marker);current.path.moveTo(x,y);getParent().requestDisallowInterceptTouchEvent(true);invalidate();return true;case MotionEvent.ACTION_MOVE:if(current!=null){current.path.lineTo(x,y);invalidate();}return true;case MotionEvent.ACTION_UP:if(current!=null){current.path.lineTo(x,y);ink.computeIfAbsent(index,k->new ArrayList<>()).add(current);redoInk.remove(index);current=null;invalidate();}getParent().requestDisallowInterceptTouchEvent(false);return true;case MotionEvent.ACTION_CANCEL:current=null;getParent().requestDisallowInterceptTouchEvent(false);invalidate();return true;}return true;}
 }
 void inkButton(LinearLayout target,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextSize(11);target.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));b.setOnClickListener(v->action.run());}
 void toggleInk(){annotating=!annotating;annotationBar.setVisibility(annotating?View.VISIBLE:View.GONE);if(annotating){controlsVisible=true;header.setVisibility(View.VISIBLE);footer.setVisibility(View.VISIBLE);hideHandler.removeCallbacksAndMessages(null);}else{scheduleHide();}render();}
 void undoInk(){ArrayList<InkStroke> a=ink.get(page);if(a!=null&&!a.isEmpty()){redoInk.computeIfAbsent(page,k->new ArrayList<>()).add(a.remove(a.size()-1));render();}}
 void redoInk(){ArrayList<InkStroke> a=redoInk.get(page);if(a!=null&&!a.isEmpty()){ink.computeIfAbsent(page,k->new ArrayList<>()).add(a.remove(a.size()-1));render();}}
 void exportAnnotated(){if(renderer==null){toast("Buka PDF dahulu");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"JAMS-Anotasi.pdf");startActivityForResult(i,EXPORT);}
 void writeAnnotated(OutputStream out)throws IOException {PdfDocument doc=new PdfDocument();try{for(int j=0;j<renderer.getPageCount();j++){PdfRenderer.Page original=renderer.openPage(j);try{int pw=original.getWidth(),ph=original.getHeight();float factor=Math.min(1f,1800f/Math.max(pw,ph));int bw=Math.max(1,(int)(pw*factor)),bh=Math.max(1,(int)(ph*factor));Bitmap bmp=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);original.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_PRINT);PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(pw,ph,j+1).create());Canvas c=pg.getCanvas();c.drawBitmap(bmp,null,new Rect(0,0,pw,ph),new Paint(3));drawInk(c,j,pw,ph);doc.finishPage(pg);bmp.recycle();}finally{original.close();}}doc.writeTo(out);}finally{doc.close();}}
 ImageView image; ImageView image; TextView status; LinearLayout root; LinearLayout header, footer; android.os.Handler hideHandler=new android.os.Handler(android.os.Looper.getMainLooper()); boolean controlsVisible=true; float touchX,touchY; boolean pinching=false;
 LinearLayout pages; ScrollView scroll; ScaleGestureDetector scaleDetector; boolean flip=true; int baseTop,baseBottom;
 ArrayList<Bitmap> scans=new ArrayList<>(); File photoFile;
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 void button(LinearLayout row,String text,Runnable fn){Button b=new Button(this);b.setText(text);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->fn.run());}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);root.addView(l);return l;}
 int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
 void safeArea(){ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{androidx.core.graphics.Insets i=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());root.setPadding(i.left, i.top+dp(6),i.right,i.bottom+dp(6));return insets;});ViewCompat.requestApplyInsets(root);}
 void toggleControls(){controlsVisible=!controlsVisible;header.setVisibility(controlsVisible?View.VISIBLE:View.GONE);footer.setVisibility(controlsVisible?View.VISIBLE:View.GONE);if(controlsVisible&&!annotating)scheduleHide();}
 void scheduleHide(){hideHandler.removeCallbacksAndMessages(null);hideHandler.postDelayed(()->{if(renderer!=null&&controlsVisible&&!annotating){controlsVisible=false;header.setVisibility(View.GONE);footer.setVisibility(View.GONE);}},3500);}
 @Override public void onCreate(Bundle b){super.onCreate(b);
 getWindow().setStatusBarColor(Color.rgb(20,32,48));getWindow().setNavigationBarColor(Color.rgb(20,32,48));
 root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.rgb(29,36,46));
 header=new LinearLayout(this);header.setOrientation(1);root.addView(header);
 TextView title=new TextView(this);title.setText("JAMS PDF  •  eBook v1.3");title.setTextSize(18);title.setTextColor(Color.WHITE);title.setPadding(dp(16),dp(10),0,dp(8));header.addView(title);
 LinearLayout top=new LinearLayout(this);header.addView(top);addButton(top,"BUKA",this::pick);addButton(top,"SCAN",this::scan);addButton(top,"CETAK",this::print);addButton(top,"KONGSI",this::share);
 scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.rgb(45,51,61));
 pages=new LinearLayout(this);pages.setOrientation(1);pages.setGravity(Gravity.CENTER_HORIZONTAL);pages.setPadding(dp(5),dp(5),dp(5),dp(5));scroll.addView(pages);
 root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
 footer=new LinearLayout(this);footer.setOrientation(1);root.addView(footer);
 annotationBar=new LinearLayout(this);annotationBar.setOrientation(LinearLayout.VERTICAL);footer.addView(annotationBar);annotationBar.setVisibility(View.GONE);
 LinearLayout tools=new LinearLayout(this);annotationBar.addView(tools);inkButton(tools,"PEN",()->{marker=false;inkColor=Color.RED;});inkButton(tools,"HIGHLIGHT",()->{marker=true;inkColor=Color.YELLOW;inkWidth=22f;});inkButton(tools,"UNDO",this::undoInk);inkButton(tools,"REDO",this::redoInk);
 LinearLayout colors=new LinearLayout(this);annotationBar.addView(colors);inkButton(colors,"MERAH",()->{inkColor=Color.RED;marker=false;inkWidth=3f;});inkButton(colors,"BIRU",()->{inkColor=Color.BLUE;marker=false;inkWidth=3f;});inkButton(colors,"HIJAU",()->{inkColor=Color.GREEN;marker=false;inkWidth=3f;});inkButton(colors,"KUNING",()->{inkColor=Color.YELLOW;marker=true;inkWidth=22f;});
 LinearLayout bottom=new LinearLayout(this);footer.addView(bottom);addButton(bottom,"◀",()->changePage(-1));
 status=new TextView(this);status.setGravity(Gravity.CENTER);status.setTextColor(Color.WHITE);status.setText("Buka PDF untuk membaca");bottom.addView(status,new LinearLayout.LayoutParams(0,dp(48),3));addButton(bottom,"▶",()->changePage(1));
 LinearLayout options=new LinearLayout(this);footer.addView(options);addButton(options,"SCROLL / FLIP",()->{flip=!flip;render();toast(flip?"Mod eBook: swipe kiri/kanan":"Mod scroll: swipe atas/bawah");scheduleHide();});addButton(options,"FIT",()->{zoom=1f;render();scheduleHide();});addButton(options,"TULIS",this::toggleInk);addButton(options,"SIMPAN NOTA",this::exportAnnotated);
 setContentView(root);safeArea();
 scaleDetector=new ScaleGestureDetector(this,new ScaleGestureDetector.SimpleOnScaleGestureListener(){@Override public boolean onScale(ScaleGestureDetector d){zoom=Math.max(1f,Math.min(5f,zoom*d.getScaleFactor()));pinching=true;return true;}@Override public void onScaleEnd(ScaleGestureDetector d){render(false);pinching=false;}});
 Uri u=getIntent().getData();if(u!=null)load(u);
 }
 void addButton(LinearLayout target,String label,Runnable action){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);target.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));b.setOnClickListener(v->{action.run();scheduleHide();});}
 void changePage(int delta){if(renderer==null)return;page=Math.max(0,Math.min(renderer.getPageCount()-1,page+delta));render();scheduleHide();}
 @Override public boolean dispatchTouchEvent(MotionEvent e){if(renderer!=null&&!annotating){scaleDetector.onTouchEvent(e);int action=e.getActionMasked();if(action==MotionEvent.ACTION_DOWN){touchX=e.getX();touchY=e.getY();}if(action==MotionEvent.ACTION_UP&&!pinching){float dx=e.getX()-touchX,dy=e.getY()-touchY;if(flip&&zoom<=1.05f&&Math.abs(dx)>dp(65)&&Math.abs(dx)>Math.abs(dy)*1.35f){changePage(dx<0?1:-1);return true;}if(Math.abs(dx)<dp(15)&&Math.abs(dy)<dp(15)&&e.getY()>dp(80)&&e.getY()<root.getHeight()-dp(75)){toggleControls();return true;}}}return super.dispatchTouchEvent(e);}
 void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);}
 void load(Uri u){try{closePdf();descriptor=getContentResolver().openFileDescriptor(u,"r");renderer=new PdfRenderer(descriptor);pdfUri=u;page=0;zoom=1;ink.clear();redoInk.clear();annotating=false;annotationBar.setVisibility(View.GONE);render();scheduleHide();}catch(Exception e){toast("PDF gagal dibuka: "+e.getMessage());}}
 void render(){render(true);}
 void render(boolean resetScroll){if(renderer==null)return;int sy=scroll.getScrollY();pages.removeAllViews();int count=renderer.getPageCount();int start=flip?page:Math.max(0,page-1),end=flip?page+1:Math.min(count,page+4);
 int screen=getResources().getDisplayMetrics().widthPixels-dp(18);int w=Math.max(240,Math.min(3000,(int)(screen*zoom)));
 for(int j=start;j<end;j++){PdfRenderer.Page pg=null;try{pg=renderer.openPage(j);int rw=w;int h=Math.max(1,(int)((double)pg.getHeight()*w/pg.getWidth()));long pixels=(long)w*h;if(pixels>8000000L){double factor=Math.sqrt(8000000.0/pixels);rw=(int)(w*factor);h=(int)(h*factor);}Bitmap bmp=Bitmap.createBitmap(rw,h,Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);pg.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);AnnotatedPage iv=new AnnotatedPage(bmp,j);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(Math.max(1,(int)(rw/getResources().getDisplayMetrics().density))),-2);lp.bottomMargin=dp(12);pages.addView(iv,lp);TextView marker=new TextView(this);marker.setText("Halaman "+(j+1)+" / "+count);marker.setGravity(Gravity.CENTER);marker.setTextColor(Color.DKGRAY);pages.addView(marker);}catch(Exception e){toast("Gagal render halaman "+(j+1)+": "+e.getMessage());}finally{if(pg!=null)pg.close();}}
 status.setText((page+1)+" / "+count+(flip?"  •  eBook":"  •  Scroll"));scroll.post(()->scroll.scrollTo(0,resetScroll?0:sy));}
 void scan(){try{photoFile=File.createTempFile("jams_scan_",".jpg",getCacheDir());cameraUri=FileProvider.getUriForFile(this,getPackageName()+".files",photoFile);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){toast("Kamera tidak tersedia: "+e.getMessage());}}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(result!=RESULT_OK)return;
 if(r==OPEN&&data!=null)load(data.getData());else if(r==CAMERA){try{BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeFile(photoFile.getAbsolutePath(),opt);opt.inSampleSize=Math.max(1,Math.max(opt.outWidth,opt.outHeight)/1800);opt.inJustDecodeBounds=false;Bitmap src=BitmapFactory.decodeFile(photoFile.getAbsolutePath(),opt);if(src==null)throw new IOException("Gambar kosong");Bitmap cropped=AutoCrop.crop(src);scans.add(cropped);pages.removeAllViews();ImageView preview=new ImageView(this);preview.setImageBitmap(cropped);preview.setAdjustViewBounds(true);pages.addView(preview);status.setText("Scan "+scans.size()+" halaman");new AlertDialog.Builder(this).setTitle("Auto Scan selesai").setMessage("Semak hasil crop. Tambah halaman atau simpan PDF?").setPositiveButton("Simpan PDF",(d,w)->saveScan()).setNeutralButton("Tambah",(d,w)->scan()).setNegativeButton("Buang halaman",(d,w)->{scans.remove(scans.size()-1);}).show();}catch(Exception e){toast("Scan gagal: "+e.getMessage());}}
 else if(r==EXPORT&&data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){writeAnnotated(out);toast("PDF dengan anotasi disimpan");}catch(Exception e){toast("Eksport gagal: "+e.getMessage());}}
 else if(r==SAVE&&data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){writeScans(out);toast("PDF berjaya disimpan");scans.clear();load(data.getData());}catch(Exception e){toast("Simpan gagal: "+e.getMessage());}}}
 void saveScan(){if(scans.isEmpty()){toast("Tiada scan");return;}Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,"JAMS-Scan-"+System.currentTimeMillis()+".pdf");startActivityForResult(i,SAVE);}
 void writeScans(OutputStream out)throws IOException{PdfDocument doc=new PdfDocument();try{for(int j=0;j<scans.size();j++){Bitmap b=scans.get(j);int pw=595,ph=842;PdfDocument.Page pg=doc.startPage(new PdfDocument.PageInfo.Builder(pw,ph,j+1).create());Canvas c=pg.getCanvas();c.drawColor(Color.WHITE);float scale=Math.min((pw-36f)/b.getWidth(),(ph-36f)/b.getHeight());float w=b.getWidth()*scale,h=b.getHeight()*scale;c.drawBitmap(b,null,new RectF((pw-w)/2,(ph-h)/2,(pw+w)/2,(ph+h)/2),new Paint(3));doc.finishPage(pg);}doc.writeTo(out);}finally{doc.close();}}
 void share(){if(pdfUri==null){toast("Buka atau simpan PDF dahulu");return;}Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,pdfUri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"Kongsi fail PDF"));}
 void print(){if(renderer==null||pdfUri==null){toast("Buka PDF dahulu");return;}final Uri target=pdfUri;PrintManager pm=(PrintManager)getSystemService(PRINT_SERVICE);pm.print("JAMS PDF",new PrintDocumentAdapter(){
 @Override public void onLayout(PrintAttributes old,PrintAttributes now,CancellationSignal cancel,LayoutResultCallback callback,Bundle extras){if(cancel.isCanceled()){callback.onLayoutCancelled();return;}PrintDocumentInfo info=new PrintDocumentInfo.Builder("JAMS-document.pdf").setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(renderer.getPageCount()).build();callback.onLayoutFinished(info,true);}
 @Override public void onWrite(PageRange[] ranges,ParcelFileDescriptor dest,CancellationSignal cancel,WriteResultCallback callback){try(InputStream in=getContentResolver().openInputStream(target);OutputStream out=new FileOutputStream(dest.getFileDescriptor())){byte[] buf=new byte[16384];int n;while((n=in.read(buf))!=-1){if(cancel.isCanceled()){callback.onWriteCancelled();return;}out.write(buf,0,n);}out.flush();callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});}catch(Exception e){callback.onWriteFailed(e.getMessage());}}
 },null);}
 void closePdf(){if(renderer!=null){renderer.close();renderer=null;}if(descriptor!=null){try{descriptor.close();}catch(Exception ignored){}descriptor=null;}}
 @Override protected void onDestroy(){closePdf();super.onDestroy();}
}
