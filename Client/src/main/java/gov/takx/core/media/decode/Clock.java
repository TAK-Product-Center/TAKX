package gov.takx.core.media.decode;

/**
 * Clock to manage real-time decoding
 *
 * @since 2.8
 */
public class Clock
{
    double pts = 0L;
    double ptsDrift = 0L;
    double lastUpdated = 0L;
    double speed = 1.0;
    int serial = 0;
    volatile boolean paused = false;

    public Clock()
    {
        setClock(Double.NaN, -1);
    }

    public synchronized double getClock()
    {
        if (paused)
        {
            return pts;
        } else
        {
            double time = timeInSeconds();
            return ptsDrift + time - (time - lastUpdated) * (1.0 - speed);
        }
    }

    public void setClock(double pts, int serial)
    {
        setClockAt(pts, serial, timeInSeconds());
    }

    public synchronized void setClockAt(double pts, int serial, double time)
    {
        this.pts = pts;
        this.serial = serial;
        this.lastUpdated = time;
        this.ptsDrift = pts - time;
    }

    public double getPts()
    {
        return pts;
    }

    public double getPtsDrift()
    {
        return ptsDrift;
    }

    public double getLastUpdated()
    {
        return lastUpdated;
    }

    public double getSpeed()
    {
        return speed;
    }

    public int getSerial()
    {
        return serial;
    }

    public boolean isPaused()
    {
        return paused;
    }

    public void setPaused(boolean paused)
    {
        this.paused = paused;
    }

    public static double timeInSeconds()
    {
        return System.nanoTime() / 1000000000.0;
    }

    public static double timeInMicroseconds()
    {
        return System.nanoTime() / 1000000.0;
    }
}
