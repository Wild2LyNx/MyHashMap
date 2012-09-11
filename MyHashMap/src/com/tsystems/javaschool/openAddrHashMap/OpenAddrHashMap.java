package com.tsystems.javaschool.openAddrHashMap;

import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

public class OpenAddrHashMap<K, V> extends AbstractMap<K, V> implements
		Map<K, V>, Cloneable {
	private static final int INITIAL_SIZE = 16;
	private static final float LOAD_FACTOR = 0.75f;

	static class MyEntry<K, V> implements java.util.Map.Entry<K, V> {

		final K key;
		V value;
		MyEntry<K, V> next;

		MyEntry(K key, V value, MyEntry<K, V> next) {
			this.key = key;
			this.value = value;
			this.next = next;
		}

		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V arg) {
			V prev = value;
			value = arg;
			return prev;
		}

		public final String toString() {
			return getKey() + "=" + getValue();
		}

		void recordRemoval(OpenAddrHashMap<K, V> m) {
			m.remove(key);
		}

	}

	int capacity;
	float loadFactor;
	int size = 0;
	private MyEntry<K, V>[] storage;
	private Set<java.util.Map.Entry<K, V>> entrySet;
	public transient volatile int modCount;

	static int indexFor(int h, int length) {
		return h & (length - 1);
	}

	@SuppressWarnings("unchecked")
	public OpenAddrHashMap(int capacity, float loadFactor) {
		capacity = primeUp(capacity);
		this.capacity = capacity;
		storage = new MyEntry[capacity];
		this.loadFactor = loadFactor;
	}

	public OpenAddrHashMap(int capacity) {
		this(capacity, LOAD_FACTOR);
	}

	public OpenAddrHashMap() {
		this(INITIAL_SIZE, LOAD_FACTOR);
	}

	public int size() {
		return size;
	}

	int hash(Object obj) {
		return Math.abs(obj.hashCode() % capacity);
	}

	@SuppressWarnings("rawtypes")
	MyEntry search(K key) {
		int num = hash(key);
		MyEntry e = (MyEntry) storage[num];
		while (e != null) {
			if (e.key.equals(key)) {
				break;
			} else {
				e = e.next;
			}
		}
		return e;
	}

	@SuppressWarnings("unchecked")
	public V put(K key, V value) {
		MyEntry<K, V> foundAt = search(key);
		if (foundAt != null) {
			V prev = (V) foundAt.value;
			foundAt.value = value;
			return prev;
		} else {
			++size;
			float load = (float) size / capacity;

			if (load > loadFactor) {
				rehash();
			}
			int num = hash(key);
			storage[num] = new MyEntry<K, V>(key, value,
					(MyEntry<K, V>) storage[num]);
			modCount++;
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public V get(Object k) {
		K key = (K) k;
		MyEntry<K, V> foundAt = search(key);
		if (foundAt != null) {
			return (V) foundAt.value;
		} else {
			return null;
		}
	}

	private int primeUp(int n) {
		int prime = n;
		if (prime % 2 == 0) {
			prime += 1;
		}
		boolean found_prime;
		do {
			found_prime = true;
			for (int i = 3; i <= Math.sqrt(prime); i += 2) {
				if (prime % i == 0) {
					found_prime = false;
					prime += 2;
					break;
				}
			}
		} while (!found_prime);
		return prime;
	}

	@SuppressWarnings("unchecked")
	private void rehash() {
		List<MyEntry<K, V>> saved_nodes = new ArrayList<MyEntry<K, V>>(size);
		for (int i = 0; i < capacity; ++i) {
			MyEntry<K, V> curr = (MyEntry<K, V>) storage[i];
			for (; curr != null; curr = curr.next) {
				saved_nodes.add(curr);
			}
		}
		capacity = primeUp(2 * capacity);
		storage = new MyEntry[capacity];
		for (MyEntry<K, V> node : saved_nodes) {
			int new_num = hash(node.key);
			node.next = (MyEntry<K, V>) storage[new_num];
			storage[new_num] = node;
		}
	}

	final MyEntry<K, V> removeEntryForKey(Object key) {
		int hash = (key == null) ? 0 : hash(key.hashCode());
		int i = indexFor(hash, storage.length);
		MyEntry<K, V> prev = storage[i];
		MyEntry<K, V> e = prev;

		while (e != null) {
			MyEntry<K, V> next = e.next;
			Object k;
			if (e.hashCode() == hash
					&& ((k = e.key) == key || (key != null && key.equals(k)))) {
				modCount++;
				size--;
				if (prev == e)
					storage[i] = next;
				else
					prev.next = next;
				e.recordRemoval(this);
				return e;
			}
			prev = e;
			e = next;
		}

		return e;
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		if (entrySet == null) {
			entrySet = new MyEntrySet();
		}
		return entrySet;
	}

	class MyEntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<java.util.Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		private abstract class HashIterator<E> implements Iterator<E> {
			MyEntry<K, V> next;
			int expectedModCount;
			int index;
			MyEntry<K, V> current;

			@SuppressWarnings("unchecked")
			HashIterator() {
				expectedModCount = modCount;
				if (size > 0) {
					@SuppressWarnings("rawtypes")
					MyEntry[] s = storage;
					while (index < s.length && (next = s[index++]) == null)
						;
				}
			}

		}

		private final class EntryIterator extends HashIterator<Map.Entry<K, V>> {
			public Map.Entry<K, V> next() {
				return nextEntry();
			}

			@Override
			public boolean hasNext() {
				return next != null;
			}

			@SuppressWarnings("unchecked")
			final MyEntry<K, V> nextEntry() {
				if (modCount != expectedModCount)
					throw new ConcurrentModificationException();
				MyEntry<K, V> t = next;
				if (t == null)
					throw new NoSuchElementException();

				if ((next = t.next) == null) {
					@SuppressWarnings("rawtypes")
					MyEntry[] s = storage;
					while (index < s.length && (next = s[index++]) == null)
						;
				}
				current = t;
				return t;
			}

			@Override
			public void remove() {
				if (current == null)
					throw new IllegalStateException();
				if (modCount != expectedModCount)
					throw new ConcurrentModificationException();
				Object key = current.key;
				current = null;
				OpenAddrHashMap.this.removeEntryForKey(key);
				expectedModCount = modCount;

			}

		}

		@Override
		public int size() {
			return size;
		}
	}
}
