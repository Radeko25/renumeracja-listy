package pl.com.razi.listy.przesuwanie.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga;
import pl.com.razi.listy.przesuwanie.PrzesuwanieObsluga.TrybPrzesuwania;
import pl.com.razi.listy.przesuwanie.wynik.PrzesuwanieWynik;

public class PrzesuwanieSwingTest {

	private static final int LICZBA_WIERSZY_TABELI = 100_000;

	// ================================
	// Listener pomocniczy
	// ================================
	@FunctionalInterface
	interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
		void update(javax.swing.event.DocumentEvent e);

		default void insertUpdate(javax.swing.event.DocumentEvent e) {
			update(e);
		}

		default void removeUpdate(javax.swing.event.DocumentEvent e) {
			update(e);
		}

		default void changedUpdate(javax.swing.event.DocumentEvent e) {
			update(e);
		}
	}

	private JFrame frame;
	private JTable table;
	private DefaultTableModel model;

	private JComboBox<TrybPrzesuwania> comboTryb;
	private JTextField polePrzesuniecie;

	JButton btnGora = new JButton("GÓRA");
	JButton btnDol = new JButton("DÓŁ");

	private PrzesuwanieObsluga<Integer> obsluga;

	// ===========================================
	// KLUCZOWE — TRZYMAMY LISTĘ TYLKO TUTAJ
	// ===========================================
	private List<Integer> aktualnaLista = new ArrayList<>();

	// Listener zapamiętany, aby można było go wyłączyć
	private ListSelectionListener selectionListener;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> new PrzesuwanieSwingTest().start());
	}

	private void start() {

		// ============================
		// INSTANCJA OBSŁUGI
		// ============================
		obsluga = new PrzesuwanieObsluga.Builder<Integer>().trybPrzesuwania(TrybPrzesuwania.CYKLICZNE).build();

		frame = new JFrame("Test przesuwania – Swing");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(600, 400);

		// ============================
		// MODELOWANIE TABELI
		// ============================
		model = new DefaultTableModel(new Object[] { "Lp" }, 0) {
			@Override
			public boolean isCellEditable(int r, int c) {
				return false;
			}
		};

		// Ładujemy listę JEDEN RAZ
		for (int i = 1; i <= LICZBA_WIERSZY_TABELI; i++) {
			aktualnaLista.add(i);
		}
		odswiezTabeleBezZaznaczenia();

		table = new JTable(model);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

		// ============================
		// LISTENER ZAZNACZENIA
		// ============================
		selectionListener = e -> {
			if (!e.getValueIsAdjusting()) {
				aktualizujAktywnoscPrzyciskow();
			}
		};
		table.getSelectionModel().addListSelectionListener(selectionListener);

		frame.add(new JScrollPane(table), BorderLayout.CENTER);

		// ============================
		// PANEL STERUJĄCY
		// ============================
		JPanel panel = new JPanel(new FlowLayout());

		comboTryb = new JComboBox<>(TrybPrzesuwania.values());
		comboTryb.setSelectedItem(TrybPrzesuwania.CYKLICZNE);
		comboTryb.addActionListener(e -> {
			obsluga.ustawTrybPrzesuwania((TrybPrzesuwania) comboTryb.getSelectedItem());
			aktualizujAktywnoscPrzyciskow();
		});

		polePrzesuniecie = new JTextField("1", 4);
		polePrzesuniecie.getDocument()
				.addDocumentListener((SimpleDocumentListener) e -> aktualizujAktywnoscPrzyciskow());

		btnGora.addActionListener(e -> wykonajPrzesuniecie(-pobierzPrzesuniecie()));
		btnDol.addActionListener(e -> wykonajPrzesuniecie(+pobierzPrzesuniecie()));

		panel.add(new JLabel("Tryb:"));
		panel.add(comboTryb);
		panel.add(new JLabel("Przesunięcie:"));
		panel.add(polePrzesuniecie);
		panel.add(btnGora);
		panel.add(btnDol);

		frame.add(panel, BorderLayout.SOUTH);

		frame.setVisible(true);
	}

	private int pobierzPrzesuniecie() {
		try {
			return Integer.parseInt(polePrzesuniecie.getText());
		} catch (Exception e) {
			return 1;
		}
	}

	private void wykonajPrzesuniecie(int przesuniecie) {
		try {
			List<Integer> wybrane = pobierzWybrane();

			PrzesuwanieWynik<Integer> wynik = obsluga.przesunPelny(aktualnaLista, wybrane, przesuniecie);
			aktualnaLista = wynik.getLista();

			System.out.println(wynik.getPlan().toSqlCaseWhenBloki("Tabela", "kolumna", null));

			odswiezTabele(aktualnaLista, wybrane);
			aktualizujAktywnoscPrzyciskow();

		} catch (Exception ex) {
			ex.printStackTrace();
			JOptionPane.showMessageDialog(frame, "Błąd: " + ex.getMessage());
		}
	}

	private List<Integer> pobierzWybrane() {
		List<Integer> list = new ArrayList<>();
		for (int row : table.getSelectedRows()) {
			list.add((Integer) model.getValueAt(row, 0));
		}
		return list;
	}

	// ============================
	// ODŚWIEŻANIE TABELI
	// ============================
	private void odswiezTabeleBezZaznaczenia() {
		model.setRowCount(0);
		for (Integer i : aktualnaLista) {
			model.addRow(new Object[] { i });
		}
	}

	private void odswiezTabele(List<Integer> nowe, List<Integer> stareZaznaczenie) {

		// 1. Zamieniamy zaznaczenie na HashSet — BŁYSKAWICZNE contains()
		Set<Integer> zaznaczone = new HashSet<>(stareZaznaczenie);

		// 2. Wyłączamy listener zaznaczenia (inaczej odpali 10k razy!)
		table.getSelectionModel().removeListSelectionListener(selectionListener);

		// 3. Odtwarzamy tabelę
		model.setRowCount(0);
		for (Integer i : nowe) {
			model.addRow(new Object[] { i });
		}

		// 4. Odtwarzamy zaznaczenie — TERAZ SZYBKIE
		table.clearSelection();
		for (int row = 0; row < model.getRowCount(); row++) {
			Integer val = (Integer) model.getValueAt(row, 0);
			if (zaznaczone.contains(val)) {
				table.addRowSelectionInterval(row, row);
			}
		}

		// 5. Włączamy listener z powrotem
		table.getSelectionModel().addListSelectionListener(selectionListener);
	}

	// ============================
	// AKTYWACJA PRZYCISKÓW
	// ============================
	private void aktualizujAktywnoscPrzyciskow() {

		try {
			List<Integer> wybrane = pobierzWybrane();

			if (wybrane.isEmpty()) {
				ustawAktywnosc(false, false);
				return;
			}

			int przes = pobierzPrzesuniecie();

			boolean gora = obsluga.czyDoPrzesunieciaDojdzie(aktualnaLista, wybrane, -przes);
			boolean dol = obsluga.czyDoPrzesunieciaDojdzie(aktualnaLista, wybrane, +przes);

			ustawAktywnosc(gora, dol);

		} catch (Exception e) {
			ustawAktywnosc(false, false);
		}
	}

	private void ustawAktywnosc(boolean gora, boolean dol) {
		btnGora.setEnabled(gora);
		btnDol.setEnabled(dol);
	}

}
