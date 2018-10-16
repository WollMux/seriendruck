package de.muenchen.mailmerge;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.container.XSet;
import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.UnoRuntime;

import de.muenchen.allg.afid.UNO;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.LogConfig;
import de.muenchen.mailmerge.comp.MailMergeComponent;

/**
 * Über diese Klasse kann der WollMux zum Debuggen in der lokalen JVM gestartet
 * werden, ohne dass die Extension in OpenOffice/LibreOffice installiert ist.
 * Bisher haben wir für diesen Zweck die WollMuxBar mit der Konfigurationsoption
 * ALLOW_EXTERNAL_WOLLMUX "true" verwendet. Damit das Debuggen auch ohne
 * WollMuxBar möglich ist, wurde diese Klasse eingefügt. Zusammen mit dem neuen
 * ant build-target WollMux.oxt-ButtonsOnly kann so das Debugging vereinfacht
 * werden.
 * 
 * Verwendung: Diese Main-Methode einfach per Debugger starten.
 * 
 * @author Christoph
 * 
 */
public class DebugExternalMailMerge
{

  private static final Logger LOGGER = LoggerFactory.getLogger(DebugExternalMailMerge.class);

  public static void main(String[] args) throws Exception
  {
    // Logger zum Debuggen auf stdout initialisieren und die zukünftigen
    // Logger-Einstellungen aus der wollmuxconfig ignorieren.
    LogConfig.init(System.out, Level.DEBUG);
    LogConfig.setIgnoreInit(true);

    UNO.init();

    UNO.desktop.addTerminateListener(new XTerminateListener()
    {

      @Override
      public void disposing(EventObject arg0)
      {
      }

      @Override
      public void queryTermination(EventObject arg0) throws TerminationVetoException
      {
      }

      @Override
      public void notifyTermination(EventObject arg0)
      {
        LOGGER.info(L.m("Desktop wurde geschlossen - beende DebugExternalWollMux"));
        System.exit(0);
      }
    });

    XSet set = UnoRuntime.queryInterface(XSet.class, UNO.defaultContext.getServiceManager());
    set.insert(MailMergeComponent.__getComponentFactory(MailMergeComponent.class.getName()));

    new MailMergeComponent(UNO.defaultContext);
  }
}
