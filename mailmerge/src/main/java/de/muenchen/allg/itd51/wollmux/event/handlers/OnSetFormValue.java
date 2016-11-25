package de.muenchen.allg.itd51.wollmux.event.handlers;

import java.awt.event.ActionListener;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.WollMuxFehlerException;
import de.muenchen.allg.itd51.wollmux.document.DocumentManager;
import de.muenchen.allg.itd51.wollmux.document.TextDocumentController;

public class OnSetFormValue extends BasicEvent
{
  private XTextDocument doc;

  private String id;

  private String value;

  private final ActionListener listener;

  public OnSetFormValue(XTextDocument doc, String id, String value,
      ActionListener listener)
  {
    this.doc = doc;
    this.id = id;
    this.value = value;
    this.listener = listener;
  }

  @Override
  protected void doit() throws WollMuxFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);

    // Werte selber setzen:
    documentController.addFormFieldValue(id, value);
    if (listener != null)
      listener.actionPerformed(null);
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", id='" + id
      + "', value='" + value + "')";
  }
}