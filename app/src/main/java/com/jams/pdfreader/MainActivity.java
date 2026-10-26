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
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 static final int OPEN=1,CAMERA=2,SAVE=3;
 Uri pdfUri, cameraUri; PdfRenderer renderer; ParcelFileDescriptor descriptor;
 int page=0; float zoom=1f; ImageView image; TextView status; LinearLayout root;
 ArrayList<Bitmap> scans=new ArrayList<>(); File photoFile;
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 void button(LinearLayout row,String text,Runnable fn){Button b=new Button(this);b.setText(text);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->fn.run());}
 LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);root.addView(l);return l;}
 @Override public void onCreate(Bundle b){super.onCreate(b);root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.WHITE);
 LinearLayout top=row();button(top,"Buka",this::pick);button(top,"Scan",this::scan);button(top,"Print",this::print);button(top,"Kongsi",this::share);
 ScrollView scroll=new ScrollView(this);image=new ImageView(this);image.setAdjustViewBounds(true);scroll.addView(image);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
 LinearLayout bottom=row();button(bottom,"◀",()->{if(page>0){page--;render();}});button(bottom,"−",()->{zoom=Math.max(.6f,zoom/1.3f);render();});status=new TextView(this);status.setGravity(Gravity.CENTER);bottom.addView(status,new LinearLayout.LayoutParams(0,-1,2));button(bottom,"+",()->{zoom=Math.min(3f,zoom*1.3f);render();});button(bottom,"▶",()->{if(renderer!=null&&page+1<renderer.getPageCount()){page++;render();}});
 setContentView(root);Uri u=getIntent().getData();if(u!=null)load(u);else status.setText("Buka PDF atau Scan dokumen");}
 void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/pdf");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,OPEN);}
 void load(Uri u){try{closePdf();descriptor=getContentResolver().openFileDescriptor(u,"r");renderer=new PdfRenderer(descriptor);pdfUri=u;page=0;zoom=1;render();}catch(Exception e){toast("PDF gagal dibuka: "+e.getMessage());}}
 void render(){if(renderer==null)return;PdfRenderer.Page p=null;try{p=renderer.openPage(page);int w=Math.min(2400,Math.max(300,(int)(getResources().getDisplayMetrics().widthPixels*zoom)));int h=Math.max(1,(int)((double)p.getHeight()*w/p.getWidth()));Bitmap bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bmp.eraseColor(Color.WHITE);p.render(bmp,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);image.setImageBitmap(bmp);status.setText((page+1)+" / "+renderer.getPageCount());}catch(Exception e){toast(e.getMessage());}finally{if(p!=null)p.close();}}
 void scan(){try{photoFile=File.createTempFile("jams_scan_",".jpg",getCacheDir());cameraUri=FileProvider.getUriForFile(this,getPackageName()+".files",photoFile);Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);i.putExtra(MediaStore.EXTRA_OUTPUT,cameraUri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(i,CAMERA);}catch(Exception e){toast("Kamera tidak tersedia: "+e.getMessage());}}
 @Override protected void onActivityResult(int r,int result,Intent data){super.onActivityResult(r,result,data);if(result!=RESULT_OK)return;
 if(r==OPEN&&data!=null)load(data.getData());else if(r==CAMERA){try{BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeFile(photoFile.getAbsolutePath(),opt);opt.inSampleSize=Math.max(1,Math.max(opt.outWidth,opt.outHeight)/1800);opt.inJustDecodeBounds=false;Bitmap src=BitmapFactory.decodeFile(photoFile.getAbsolutePath(),opt);if(src==null)throw new IOException("Gambar kosong");Bitmap cropped=AutoCrop.crop(src);scans.add(cropped);image.setImageBitmap(cropped);status.setText("Scan "+scans.size()+" halaman");new AlertDialog.Builder(this).setTitle("Auto Scan selesai").setMessage("Semak hasil crop. Tambah halaman atau simpan PDF?").setPositiveButton("Simpan PDF",(d,w)->saveScan()).setNeutralButton("Tambah",(d,w)->scan()).setNegativeButton("Buang halaman",(d,w)->{scans.remove(scans.size()-1);}).show();}catch(Exception e){toast("Scan gagal: "+e.getMessage());}}
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
