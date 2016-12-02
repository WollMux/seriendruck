package de.muenchen.allg.itd51.wollmux.print;

import java.io.File;

import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.IOException;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.sdb.XDocumentDataSource;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.afid.UnoProps;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Implementierung einer {@link OOoDataSource}, die als Backend ein CSV-Datei
 * verwendet.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class CsvBasedOOoDataSource extends OOoDataSource
{
  File parentDir;

  CSVDataSourceWriter dsw;

  /**
   * Erzeugt eine {@link OOoDataSource}, die als Backend eine CSV-Datei verwendet
   * und die dafür notwendige Datei (eine .csv-Datei) im Verzeichnis parentDir
   * ablegt.
   */
  public CsvBasedOOoDataSource(File parentDir)
  {
    this.parentDir = parentDir;
    this.dsw = new CSVDataSourceWriter(parentDir);
  }

  /*
   * (non-Javadoc)
   * 
   * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#
   * getDataSourceWriter()
   */
  @Override
  public DataSourceWriter getDataSourceWriter()
  {
    return dsw;
  }

  /*
   * (non-Javadoc)
   * 
   * @seede.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#
   * createXDocumentDatasource()
   */
  @Override
  public XDocumentDataSource createXDocumentDatasource()
  {
    XSingleServiceFactory dbContext =
      UNO.XSingleServiceFactory(UNO.createUNOService("com.sun.star.sdb.DatabaseContext"));
    XDocumentDataSource dataSource = null;
    if (dbContext != null) try
    {
      dataSource = UNO.XDocumentDataSource(dbContext.createInstance());
    }
    catch (Exception e)
    {
      Logger.error(e);
    }

    if (dataSource != null)
    {
      String dirURL = UNO.getParsedUNOUrl(parentDir.toURI().toString()).Complete;
      UNO.setProperty(dataSource, "URL", "sdbc:flat:" + dirURL);

      UnoProps p = new UnoProps();
      p.setPropertyValue("Extension", "csv");
      p.setPropertyValue("CharSet", "UTF-8");
      p.setPropertyValue("FixedLength", false);
      p.setPropertyValue("HeaderLine", true);
      p.setPropertyValue("FieldDelimiter", ",");
      p.setPropertyValue("StringDelimiter", "\"");
      p.setPropertyValue("DecimalDelimiter", ".");
      p.setPropertyValue("ThousandDelimiter", "");
      UNO.setProperty(dataSource, "Info", p.getProps());

      XStorable xStorable = UNO.XStorable(dataSource.getDatabaseDocument());
      XModel model = UNO.XModel(xStorable);
      URL url = null;
      File tmpFile = new File(parentDir, OOoBasedMailMerge.DATASOURCE_ODB_FILENAME);
      url = UNO.getParsedUNOUrl(tmpFile.toURI().toString());
      if (url != null && xStorable != null && model != null) try
      {
        xStorable.storeAsURL(url.Complete, model.getArgs());
      }
      catch (IOException e)
      {
        Logger.error(e);
      }
    }
    return dataSource;

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#getSize()
   */
  @Override
  public int getSize()
  {
    return dsw.getSize();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * de.muenchen.allg.itd51.wollmux.func.OOoBasedMailMerge.OOoDataSource#remove()
   */
  @Override
  public void remove()
  {
    dsw.getCSVFile().delete();
  }
}