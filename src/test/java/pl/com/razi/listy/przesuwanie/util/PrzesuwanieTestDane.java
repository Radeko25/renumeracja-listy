package pl.com.razi.listy.przesuwanie.util;

import java.util.Objects;

public class PrzesuwanieTestDane {

	private final int id;

	public PrzesuwanieTestDane(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof PrzesuwanieTestDane)) {
			return false;
		}
		PrzesuwanieTestDane other = (PrzesuwanieTestDane) obj;
		return id == other.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Element(" + id + ")";
	}

}
