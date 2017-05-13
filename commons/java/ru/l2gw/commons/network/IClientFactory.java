package ru.l2gw.commons.network;

public interface IClientFactory<T extends MMOClient<?>>
{
	public T create(MMOConnection<T> con);
}
