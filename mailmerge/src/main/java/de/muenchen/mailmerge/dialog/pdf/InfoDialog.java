package de.muenchen.mailmerge.dialog.pdf;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import de.muenchen.mailmerge.dialog.Common;

public class InfoDialog
{

  private InfoDialog(String sMessage, Throwable t,
      int margin)
  {
    // StackTrace von e in String umwandeln:
    String b = "";
    if (t != null)
    {
      b += "\n\n" + t.toString() + "\n";
      for (StackTraceElement st : t.getStackTrace())
        b += st + "\n";
    }
    String message = sMessage + b;

    // zu lange Strings ab margin Zeichen umbrechen:
    String formattedMessage = Common.wrapLines(message, margin);

    showOptionDialog(formattedMessage, "Fehler beim PDF-Druck");
  }

  private void showOptionDialog(String message, String title)
  {
    JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);

    JDialog dialog = pane.createDialog(null, title);

    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
    dialog.toFront();
  }

  /**
   * Diese Methode erzeugt einen nicht modalen Swing-Dialog zur Anzeige von
   * Informationen und kehrt sofort wieder zurück.
   *
   * @param sMessage
   *          die Nachricht, die im Dialog angezeigt werden soll.
   * @param t
   *          kann ein Throwable != null enthalten, dessen StackTrace dann zwei
   *          Leerzeilen nach sMessage ausgegeben wird, kann aber auch null
   *          sein.
   * @param margin
   *          ist margin > 0 und sind in einer Zeile mehr als margin Zeichen
   *          vorhanden, so wird der Text beim nächsten Leerzeichen umgebrochen.
   */
  public static void showInfo(final String sMessage, final Throwable t,
      final int margin)
  {
    try
    {
      new Thread(new Runnable()
      {
        @Override
        public void run()
        {
          new InfoDialog(sMessage, t, margin);
        }
      }).start();
    } catch (Exception e)
    {
    }
  }
}
