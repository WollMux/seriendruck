package de.muenchen.allg.itd51.wollmux.event.handlers;

import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

import de.muenchen.allg.itd51.wollmux.ModalDialogs;
import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.core.document.commands.DocumentCommand;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnJumpToMark extends BasicEvent
{
  private XTextDocument doc;

  private boolean msg;

  public OnJumpToMark(XTextDocument doc, boolean msg)
  {
    this.doc = doc;
    this.msg = msg;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {

    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(doc);

    XTextCursor viewCursor = documentController.getModel().getViewCursor();
    if (viewCursor == null) return;

    DocumentCommand cmd = documentController.getModel().getFirstJumpMark();

    if (cmd != null)
    {
      try
      {
        XTextRange range = cmd.getTextCursor();
        if (range != null) viewCursor.gotoRange(range.getStart(), false);
      }
      catch (java.lang.Exception e)
      {
        Logger.error(e);
      }

      boolean modified = documentController.getModel().isDocumentModified();
      cmd.markDone(true);
      documentController.getModel().setDocumentModified(modified);

      documentController.getModel().getDocumentCommands().update();

    }
    else
    {
      if (msg)
      {
        ModalDialogs.showInfoModal(L.m("WollMux"),
          L.m("Kein Platzhalter und keine Marke 'setJumpMark' vorhanden!"));
      }
    }

    stabilize();
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", " + msg
      + ")";
  }
}