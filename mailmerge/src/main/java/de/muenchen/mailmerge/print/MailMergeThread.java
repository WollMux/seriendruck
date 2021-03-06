package de.muenchen.mailmerge.print;

import java.io.File;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.beans.NamedValue;
import com.sun.star.task.XJob;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XCancellable;

import de.muenchen.allg.itd51.wollmux.core.util.L;

/**
 * A optional XCancellable mail merge thread.
 *
 * @author Jan-Marek Glogowski (ITM-I23)
 */
class MailMergeThread extends Thread
{

  private static final Logger LOGGER = LoggerFactory.getLogger(MailMergeThread.class);

  private XCancellable mailMergeCancellable = null;

  private Object result = null;

  private final XJob mailMerge;

  private final File outputDir;

  private final ArrayList<NamedValue> mmProps;

  MailMergeThread(XJob mailMerge, File outputDir, ArrayList<NamedValue> mmProps)
  {
    this.mailMerge = mailMerge;
    this.outputDir = outputDir;
    this.mmProps = mmProps;
  }

  @Override
  public void run()
  {
    try
    {
      LOGGER.debug(L.m("Starting OOo-MailMerge in Verzeichnis %1", outputDir));
      // The XCancellable mail merge interface was included in LO >= 4.3.
      mailMergeCancellable =
        UnoRuntime.queryInterface(XCancellable.class, mailMerge);
      if (mailMergeCancellable != null)
        LOGGER.debug(L.m("XCancellable interface im mailMerge-Objekt gefunden!"));
      else
        LOGGER.debug(L.m("KEIN XCancellable interface im mailMerge-Objekt gefunden!"));

      result = mailMerge.execute(mmProps.toArray(new NamedValue[mmProps.size()]));

      LOGGER.debug(L.m("Finished Mail Merge"));
    }
    catch (Exception e)
    {
      LOGGER.debug(L.m("OOo-MailMergeService fehlgeschlagen: %1", e.getMessage()));
    }
    mailMergeCancellable = null;
  }

  public synchronized void cancel()
  {
    if (mailMergeCancellable != null)
      mailMergeCancellable.cancel();
  }

  public Object getResult()
  {
    return result;
  }
}