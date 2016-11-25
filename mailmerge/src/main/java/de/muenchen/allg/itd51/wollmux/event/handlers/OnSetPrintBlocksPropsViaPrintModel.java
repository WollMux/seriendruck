package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnSetPrintBlocksPropsViaPrintModel extends BasicEvent
{
  private XTextDocument doc;

  private String blockName;

  private boolean visible;

  private boolean showHighlightColor;

  private ActionListener listener;

  public OnSetPrintBlocksPropsViaPrintModel(XTextDocument doc, String blockName,
      boolean visible, boolean showHighlightColor, ActionListener listener)
  {
    this.doc = doc;
    this.blockName = blockName;
    this.visible = visible;
    this.showHighlightColor = showHighlightColor;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController =
      DocumentManager.getTextDocumentController(doc);
    try
    {
      documentController.setPrintBlocksProps(blockName, visible, showHighlightColor);
    }
    catch (java.lang.Exception e)
    {
      errorMessage(e);
    }

    stabilize();
    if (listener != null) listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", '"
      + blockName + "', '" + visible + "', '" + showHighlightColor + "')";
  }
}