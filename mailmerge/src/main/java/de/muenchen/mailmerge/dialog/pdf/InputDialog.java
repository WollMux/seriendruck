package de.muenchen.mailmerge.dialog.pdf;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.muenchen.mailmerge.dialog.Common;

public class InputDialog
{
  private JOptionPane pane;

  /**
   * Einen neuer, nicht modaler Swing-Dialog zur Abfrage einer User-Entscheidung
   * (Ja/Nein).
   *
   * @param sTitle
   *          Titelzeile des Dialogs
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   */
  public InputDialog(String sTitle, String sMessage, int margin)
  {
    // zu lange Strings ab margin Zeichen umbrechen:
    String formattedMessage = Common.wrapLines(sMessage, margin);
    showOptionDialog(formattedMessage, sTitle,
        javax.swing.JOptionPane.YES_NO_OPTION);
  }

  /**
   * Die Antwort des InputDialogs wird abgefragt.
   *
   * @return true wenn der User die Frage positiv beantwortet hat, false sonst
   */
  public boolean askForInput()
  {
    Object selectedValue = pane.getValue();

    if (selectedValue == null)
      return false;
    if (selectedValue instanceof Integer)
      return ((Integer) selectedValue).intValue() == JOptionPane.YES_OPTION;
    return false;

  }

  private void showOptionDialog(String message, String title, int optionType)
  {
    pane = new JOptionPane(message, JOptionPane.QUESTION_MESSAGE, optionType,
        null, null, null);

    JDialog dialog = pane.createDialog(null, title);

    pane.selectInitialValue();
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
    dialog.toFront();
    dialog.dispose();
  }
}
