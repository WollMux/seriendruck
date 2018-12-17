package de.muenchen.mailmerge.event.handlers;

import com.sun.star.text.XTextDocument;

import de.muenchen.allg.itd51.wollmux.core.form.model.FormModel;
import de.muenchen.allg.itd51.wollmux.core.form.model.FormModelException;
import de.muenchen.mailmerge.MailMergeFehlerException;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;

/**
 * Erzeugt ein neues WollMuxEvent, das dafür sorgt, dass im Textdokument doc das
 * Formularfeld mit der ID id auf den Wert value gesetzt wird. Ist das Dokument ein
 * Formulardokument (also mit einer angezeigten FormGUI), so wird die Änderung über
 * die FormGUI vorgenommen, die zugleich dafür sorgt, dass von id abhängige
 * Formularfelder mit angepasst werden. Besitzt das Dokument keine
 * Formularbeschreibung, so wird der Wert direkt gesetzt, ohne Äbhängigkeiten zu
 * beachten. Nach der erfolgreichen Ausführung aller notwendigen Anpassungen wird
 * der unlockActionListener benachrichtigt.
 * 
 * Das Event wird aus den Implementierungen von XPrintModel (siehe
 * TextDocumentModel) und XWollMuxDocument (siehe compo.WollMux) geworfen, wenn
 * dort die Methode setFormValue aufgerufen wird.
 * 
 */
public class OnSetFormValue extends BasicEvent
{
  private XTextDocument doc;

  private String id;

  private String value;

  /**
   * 
   * @param doc
   *          Das Dokument, in dem das Formularfeld mit der ID id neu gesetzt
   *          werden soll.
   * @param id
   *          Die ID des Formularfeldes, dessen Wert verändert werden soll. Ist
   *          die FormGUI aktiv, so werden auch alle von id abhängigen
   *          Formularwerte neu gesetzt.
   * @param value
   *          Der neue Wert des Formularfeldes id
   */
  public OnSetFormValue(XTextDocument doc, String id, String value)
  {
    this.doc = doc;
    this.id = id;
    this.value = value;
  }

  @Override
  protected void doit() throws MailMergeFehlerException
  {
    TextDocumentController documentController = DocumentManager.getTextDocumentController(doc);
    try
    {
      FormModel model = documentController.getFormModel();
      if (model != null)
      {
        model.setValue(id, value);
      } else
      {
        // Werte selber setzen:
        documentController.addFormFieldValue(id, value);
      }
    } catch (FormModelException e)
    {
      // Werte selber setzen:
      documentController.addFormFieldValue(id, value);
    }
  }

  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "(#" + doc.hashCode() + ", id='" + id
      + "', value='" + value + "')";
  }
}