package de.muenchen.allg.itd51.wollmux.event.handlers;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.OpenExt;
import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

/**
 * Dieses Event wird vom FormModelImpl ausgelöst, wenn der Benutzer die Aktion
 * "saveTempAndOpenExt" aktiviert hat.
 * 
 * @author matthias.benkmann
 */
public class OnSaveTempAndOpenExt extends BasicEvent
{
  private String ext;

  private TextDocumentController documentController;

  public OnSaveTempAndOpenExt(TextDocumentController documentController, String ext)
  {
    this.documentController = documentController;
    this.ext = ext;
  }

  @Override
  protected void doit()
  {
    try
    {
      OpenExt openExt = new OpenExt(ext, WollMuxFiles.getWollmuxConf());
      openExt.setSource(UNO.XStorable(documentController.getModel().doc));
      openExt.storeIfNecessary();
      openExt.launch(new OpenExt.ExceptionHandler()
      {
        @Override
        public void handle(Exception x)
        {
          Logger.error(x);
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
      return;
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + documentController.getModel().hashCode() + ", " + ext
      + ")";
  }
}