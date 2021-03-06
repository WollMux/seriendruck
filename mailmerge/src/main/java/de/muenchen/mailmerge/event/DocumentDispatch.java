/*
 * Dateiname: DocumentDispatch.java
 * Projekt  : WollMux
 * Funktion : Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein DocumentModel erfordern.
 *
 * Copyright (c) 2009-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 05.11.2009 | BNK | Erstellung
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 *
 */
package de.muenchen.mailmerge.event;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XStatusListener;
import com.sun.star.text.XTextDocument;
import com.sun.star.util.URL;

import de.muenchen.allg.afid.UNO;
import de.muenchen.mailmerge.document.DocumentManager;
import de.muenchen.mailmerge.document.TextDocumentController;

/**
 * Implementiert XDispatch und kann alle Dispatch-URLs behandeln, die ein
 * DocumentModel erfordern. Nähere Infos zur Funktionsweise siehe
 * {@link BaseDispatch}.
 *
 * @author Matthias Benkmann (D-III-ITD-D101)
 */
public class DocumentDispatch extends Dispatch
{
  /**
   * Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url ist.
   *
   */
  private XDispatch origDisp;

  /**
   * Zu origDisp passender URL um das Standardverhalten auszulösen.
   */
  private com.sun.star.util.URL origUrl;

  /**
   * Der Frame des Textdokuments auf das sich alle Dispatches beziehen.
   */
  private XFrame frame;

  /**
   * Erzeugt einen neuen DocumentDispatch.
   *
   * @param origDisp
   *          Ein XDispatch-Objekt, das die ursprüngliche Standard-Aktion der URL url
   *          auslösen kann.
   * @param origUrl
   *          Zu origDisp passender URL um das Standardverhalten auszulösen.
   * @param frame
   *          der Frame des Textdokuments in dessen Kontext der Dispatch ausgeführt
   *          werden soll.
   * @author Matthias Benkmann (D-III-ITD-D101)
   *
   */
  public DocumentDispatch(XDispatch origDisp, com.sun.star.util.URL origUrl,
      XFrame frame)
  {
    this.origDisp = origDisp;
    this.origUrl = origUrl;
    this.frame = frame;
  }

  /**
   * Wenn wir ein Original-Dispatch-Objekt haben, überlassen wir diesem das managen
   * des Status.
   *
   * @see #removeStatusListener(XStatusListener, URL)
   */
  @Override
  public void addStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
      origDisp.addStatusListener(listener, url);
    else
      super.addStatusListener(listener, url);
  }

  /**
   * Wenn wir ein Original-Dispatch-Objekt haben, überlassen wir diesem das managen
   * des Status.
   *
   * @see #addStatusListener(XStatusListener, URL)
   */
  @Override
  public void removeStatusListener(XStatusListener listener, URL url)
  {
    if (origDisp != null)
      origDisp.removeStatusListener(listener, url);
    else
      super.removeStatusListener(listener, url);
  }

  private TextDocumentController getDocumentController()
  {
    XTextDocument doc = UNO.XTextDocument(frame.getController().getModel());
    if (doc != null)
    {
      return DocumentManager.getTextDocumentController(doc);
    }
    return null;
  }

  public void dispatch__uno_print(String arg, PropertyValue[] props)
  {
    MailMergeEventHandler.getInstance().handlePrint(getDocumentController(), origDisp, origUrl, props);
  }

  public void dispatch__uno_printdefault(String arg, PropertyValue[] props)
  {
    MailMergeEventHandler.getInstance().handlePrint(getDocumentController(), origDisp, origUrl, props);
  }

  public void dispatch_mailmerge_seriendruck(String arg, PropertyValue[] props)
  {
    MailMergeEventHandler.getInstance().handleSeriendruck(getDocumentController());
  }

  public boolean status_mailmerge_seriendruck()
  {
    return true;
  }
}
