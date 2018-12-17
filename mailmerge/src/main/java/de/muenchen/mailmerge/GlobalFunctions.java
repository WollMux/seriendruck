package de.muenchen.mailmerge;

import de.muenchen.allg.itd51.wollmux.core.dialog.DialogLibrary;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionFactory;
import de.muenchen.allg.itd51.wollmux.core.functions.FunctionLibrary;
import de.muenchen.mailmerge.dialog.DialogFactory;
import de.muenchen.mailmerge.func.StandardPrint;
import de.muenchen.mailmerge.print.PrintFunctionLibrary;

public class GlobalFunctions
{
  private static GlobalFunctions instance;

  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary globalFunctions;
  /**
   * Enthält die im Dokumentaktionen der wollmux,conf definierten Funktionen.
   */
  private FunctionLibrary documentActionFunctions;
  /**
   * Enthält die im Funktionsdialoge-Abschnitt der wollmux,conf definierten Dialoge.
   */
  private DialogLibrary funcDialogs;
  /**
   * Enthält die im Funktionen-Abschnitt der wollmux,conf definierten Funktionen.
   */
  private PrintFunctionLibrary globalPrintFunctions;

  private GlobalFunctions()
  {
    /*
     * Globale Funktionsdialoge parsen. ACHTUNG! Muss vor parseGlobalFunctions() erfolgen. Als
     * context wird null übergeben, weil globale Funktionen keinen Kontext haben.
     */
    funcDialogs =
        DialogFactory.parseFunctionDialogs(MailMergeFiles.getWollmuxConf(), null);

    /*
     * Globale Funktionen parsen. ACHTUNG! Verwendet die Funktionsdialoge. Diese
     * müssen also vorher geparst sein. Als context wird null übergeben, weil globale
     * Funktionen keinen Kontext haben.
     */
    globalFunctions =
      FunctionFactory.parseFunctions(MailMergeFiles.getWollmuxConf(),
        getFunctionDialogs(), null, null);

    /*
     * Globale Druckfunktionen parsen.
     */
    globalPrintFunctions =
      PrintFunctionLibrary.parsePrintFunctions(MailMergeFiles.getWollmuxConf());
    StandardPrint.addInternalDefaultPrintFunctions(globalPrintFunctions);

    /*
     * Dokumentaktionen parsen. Diese haben weder Kontext noch Dialoge.
     */
    documentActionFunctions = new FunctionLibrary(null, true);
    FunctionFactory.parseFunctions(documentActionFunctions,
      MailMergeFiles.getWollmuxConf(), "Dokumentaktionen", null, null);


  }

  public static GlobalFunctions getInstance()
  {
    if (instance == null)
      instance = new GlobalFunctions();

    return instance;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Funktionen enthält.
   */
  public FunctionLibrary getGlobalFunctions()
  {
    return globalFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die Dokumentaktionen enthält.
   */
  public FunctionLibrary getDocumentActionFunctions()
  {
    return documentActionFunctions;
  }

  /**
   * Liefert die Funktionsbibliothek, die die global definierten Druckfunktionen
   * enthält.
   */
  public PrintFunctionLibrary getGlobalPrintFunctions()
  {
    return globalPrintFunctions;
  }

  /**
   * Liefert die Dialogbibliothek, die die Dialoge enthält, die in Funktionen
   * (Grundfunktion "DIALOG") verwendung finden.
   */
  public DialogLibrary getFunctionDialogs()
  {
    return funcDialogs;
  }

}
