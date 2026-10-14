import fs from 'fs';
import path from 'path';

const HABITS = [
  {
    id: "11111111-1111-4111-8111-111111111111",
    nombre: "Lectura",
    descripcion: "Leer al menos 20 páginas al día",
    categoria: "Conocimiento",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    objetivoSemanal: 5,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "22222222-2222-4222-8222-222222222222",
    nombre: "Entrenamiento de Fuerza",
    descripcion: "Rutina de hipertrofia o calistenia",
    categoria: "Salud",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
    objetivoSemanal: 4,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "33333333-3333-4333-8333-333333333333",
    nombre: "Hidratación (+2.5L)",
    descripcion: "Consumir al menos 2.5 litros de agua al día",
    categoria: "Salud",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    objetivoSemanal: 7,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "44444444-4444-4444-8444-444444444444",
    nombre: "Trabajo Proyecto Personal",
    descripcion: "Avanzar en el desarrollo y diseño de mi app",
    categoria: "Enfoque",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
    objetivoSemanal: 4,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "55555555-5555-4555-8555-555555555555",
    nombre: "Meditacion",
    descripcion: "10 minutos de respiración y atención plena",
    categoria: "Bienestar",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    objetivoSemanal: 5,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "66666666-6666-4666-8666-666666666666",
    nombre: "Tiempo en Pantalla (< 4hs)",
    descripcion: "Mantener el uso del móvil por debajo de 4 horas",
    categoria: "Bienestar",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"],
    objetivoSemanal: 6,
    archivado: false,
    createdAt: "2026-09-07"
  },
  {
    id: "77777777-7777-4777-8777-777777777777",
    nombre: "Estudio de Francés",
    descripcion: "Práctica diaria de vocabulario y fonética",
    categoria: "Conocimiento",
    diasAparicion: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
    objetivoSemanal: 4,
    archivado: true,
    createdAt: "2026-08-03"
  }
];

// Helper to format Date as YYYY-MM-DD
function formatDate(d) {
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const dailyRecords = [];

function addRecord(habitId, dateStr, cumplido, valorAlcanzado = null) {
  dailyRecords.push({
    habitId,
    fecha: dateStr,
    cumplido,
    valorAlcanzado
  });
}

// Generate calendar days from 2026-09-07 to 2026-10-13
const startDate = new Date(2026, 8, 7); // Sep 7, 2026
const endDate = new Date(2026, 9, 13); // Oct 13, 2026

// Map of day completion plan per habit
// For each day, we can define boolean completion:
const cur = new Date(startDate);
while (cur <= endDate) {
  const dateStr = formatDate(cur);
  const dayOfWeek = cur.getDay(); // 0 = Sun, 1 = Mon, ..., 6 = Sat
  const isToday = dateStr === '2026-10-13';

  // 1. Lectura (L-D, meta 5) -> All days except some Sundays/Saturdays
  if (isToday) {
    addRecord("11111111-1111-4111-8111-111111111111", dateStr, true, null);
  } else {
    const skip = (dayOfWeek === 0 && (cur.getDate() % 2 === 0));
    addRecord("11111111-1111-4111-8111-111111111111", dateStr, !skip, null);
  }

  // 2. Fuerza (L-V, meta 4)
  if (dayOfWeek >= 1 && dayOfWeek <= 5) {
    if (isToday) {
      addRecord("22222222-2222-4222-8222-222222222222", dateStr, true, null);
    } else {
      // W37 failed (3 days): Sep 7 (Mon: true), Sep 8 (Tue: false), Sep 9 (Wed: true), Sep 10 (Thu: false), Sep 11 (Fri: true)
      if (dateStr >= '2026-09-07' && dateStr <= '2026-09-11') {
        const ok = (dayOfWeek === 1 || dayOfWeek === 3 || dayOfWeek === 5);
        addRecord("22222222-2222-4222-8222-222222222222", dateStr, ok, null);
      } else {
        const ok = (dayOfWeek !== 4); // Mon, Tue, Wed, Fri (4 days)
        addRecord("22222222-2222-4222-8222-222222222222", dateStr, ok, null);
      }
    }
  }

  // 3. Hidratación (L-D, meta 7)
  if (isToday) {
    addRecord("33333333-3333-4333-8333-333333333333", dateStr, false, 1.8);
  } else {
    // W39 broke streak (Sep 21-27: Saturday Sep 26 failed with 1.5L)
    if (dateStr === '2026-09-26') {
      addRecord("33333333-3333-4333-8333-333333333333", dateStr, false, 1.5);
    } else {
      addRecord("33333333-3333-4333-8333-333333333333", dateStr, true, 2.5);
    }
  }

  // 4. Trabajo Proyecto Personal (L-V, meta 4)
  if (dayOfWeek >= 1 && dayOfWeek <= 5) {
    if (isToday) {
      addRecord("44444444-4444-4444-8444-444444444444", dateStr, false, null);
    } else {
      // 4 days per week (skip Thursday or Wednesday)
      const ok = (dayOfWeek !== 3); // Mon, Tue, Thu, Fri
      addRecord("44444444-4444-4444-8444-444444444444", dateStr, ok, null);
    }
  }

  // 5. Meditacion (L-D, meta 5)
  if (isToday) {
    addRecord("55555555-5555-4555-8555-555555555555", dateStr, false, null);
  } else {
    // 5-6 days per week (skip Sat or Sun)
    const skip = (dayOfWeek === 6 && (cur.getDate() % 3 === 0));
    addRecord("55555555-5555-4555-8555-555555555555", dateStr, !skip, null);
  }

  // 6. Tiempo en Pantalla (< 4hs) (L-D, meta 6)
  if (isToday) {
    addRecord("66666666-6666-4666-8666-666666666666", dateStr, false, 2.5);
  } else {
    // W40 broke streak (Sep 28 - Oct 04: failed 2 days: Oct 02, Oct 04)
    if (dateStr === '2026-10-02') {
      addRecord("66666666-6666-4666-8666-666666666666", dateStr, false, 5.1);
    } else if (dateStr === '2026-10-04') {
      addRecord("66666666-6666-4666-8666-666666666666", dateStr, false, 4.8);
    } else {
      addRecord("66666666-6666-4666-8666-666666666666", dateStr, true, 3.2);
    }
  }

  cur.setDate(cur.getDate() + 1);
}

// Add some records for archived habit in August
for (let d = 3; d <= 28; d++) {
  const dayStr = `2026-08-${String(d).padStart(2, '0')}`;
  addRecord("77777777-7777-4777-8777-777777777777", dayStr, (d % 3 !== 0), null);
}

const finalData = {
  habits: HABITS,
  dailyRecords: dailyRecords,
  lastCheckedDate: "2026-10-13",
  passwordHash: "J0ajxTBU3tF7P1qsu/CwW2jk9AL66Iw2XcFD+QmRqjc=",
  passwordSalt: "hxoGMBCwBkhBatuuzHtbMw=="
};

const targetPath = path.resolve('data/habits_data.json');
fs.writeFileSync(targetPath, JSON.stringify(finalData, null, 2), 'utf-8');
console.log(`Generated ${HABITS.length} habits and ${dailyRecords.length} daily records.`);
