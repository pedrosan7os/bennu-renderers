package pt.ist.fenixWebFramework.servlets.filters.contentRewrite;

import java.io.PrintWriter;

import javax.servlet.http.HttpSession;

public class BufferedFacadPrintWriter extends PrintWriter {

    final StringBuilder stringBuilder = new StringBuilder();

    final PrintWriter printWriter;

    public BufferedFacadPrintWriter(final PrintWriter printWriter) {
        super(printWriter);
        this.printWriter = printWriter;
    }

    @Override
    public void write(final char[] cbuf) {
        stringBuilder.append(cbuf);
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) {
        stringBuilder.append(cbuf, off, len);
    }

    @Override
    public void write(final int c) {
        stringBuilder.append((char) c);
    }

    @Override
    public void write(final String str) {
        stringBuilder.append(str);
    }

    @Override
    public void write(final String str, final int off, final int len) {
        stringBuilder.append(str, off, len);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
    }

    public void writeRealResponse(HttpSession session) {
        printWriter.write(new GenericChecksumRewriter(session).rewrite(this.stringBuilder.toString()));
        printWriter.flush();
        printWriter.close();
    }

    @Deprecated
    public String getContent() {
        return stringBuilder.toString();
    }

    public void resetBuffer() {
        stringBuilder.setLength(0);
    }

}
