habit-tracker/
├── pom.xml                               # Configuración de dependencias y plugins de Maven
├── data/
│   └── habits_data.json                  # Archivo plano local (persistencia)
│
└── src/
    └── main/
        ├── java/
        │   ├── model/                    # package model;
        │   │   ├── Habit.java            # Datos del hábito y validación de frecuencias[cite: 1]
        │   │   ├── DailyRecord.java      # Cumplido/fallado y valor numérico parcial alcanzado[cite: 1]
        │   │   └── AppData.java          # Contenedor raíz para Jackson[cite: 1]
        │   │
        │   ├── repository/               # package repository;
        │   │   └── JsonHabitRepository.java # Lectura y escritura atómica en el JSON[cite: 1]
        │   │
        │   ├── service/                  # package service;
        │   │   ├── HabitService.java     # ABM, archivado y eliminación física[cite: 1]
        │   │   ├── StreakService.java    # Cálculo de rachas semanales y regla anti-trampa[cite: 1]
        │   │   └── AuthService.java      # Hashing PBKDF2, verificación de clave y reseteo
        │   │
        │   └── ui/                       # package ui;
        │       ├── HabitTrackerApp.java  # Punto de entrada (start de JavaFX) y conmutador de escenas
        │       ├── MainController.java   # SplitPane con división 1/3 y 2/3[cite: 1]
        │       │
        │       ├── auth/                 # package ui.auth;
        │       │   └── LockScreenView.java # Pantalla de bloqueo, setup inicial y reseteo
        │       │
        │       ├── hud/                  # package ui.hud;
        │       │   ├── HabitItemCell.java # Fila minimalista [ ] / [x][cite: 1]
        │       │   └── CompletedSection.java # TitledPane colapsable[cite: 1]
        │       │
        │       └── analytics/            # package ui.analytics;
        │           ├── StreakCanvas.java # Gráfico de bloques/barras semanales[cite: 1]
        │           └── CycleProgress.java # Barra de avance de la semana en curso[cite: 1]
        │
        └── resources/
            └── styles/
                └── custom.css            # Estilos CSS
