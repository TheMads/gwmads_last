package ru.l2gw.gameserver.threading;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class PriorityThreadFactory implements ThreadFactory
{
	private int _prio;
	private String _name;
	private AtomicInteger _threadNumber = new AtomicInteger(1);
	private ThreadGroup _group;

	public PriorityThreadFactory(String name, int prio)
	{
		_prio = prio;
		_name = name;
		_group = new ThreadGroup(_name);
	}

	public Thread newThread(Runnable r)
	{
		Thread t = createThread(r);
		t.setName(_name + "-" + _threadNumber.getAndIncrement());
		t.setPriority(_prio);
		return t;
	}

	public Thread createThread(Runnable r)
	{
		return new Thread(getGroup(), r);
	}

	public ThreadGroup getGroup()
	{
		return _group;
	}
}
