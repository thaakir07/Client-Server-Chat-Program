# Simple Makefile for Java Chat Application

JAVAFX_HOME=~/javafx/javafx-sdk-23.0.2
SRC=src
BIN=bin

# Compiler flags for JavaFX
JAVAC_FLAGS=--module-path $(JAVAFX_HOME)/lib --add-modules javafx.controls
JAVA_FLAGS=--module-path $(JAVAFX_HOME)/lib --add-modules javafx.controls -cp $(BIN)

# Ensure bin directory exists
$(BIN):
	mkdir -p $(BIN)

# Compile Java files
compile: $(BIN)
	javac $(JAVAC_FLAGS) -d $(BIN) $(SRC)/*.java

# Run the server (compile first)
server: compile
	@(java $(JAVA_FLAGS) Server) 

# Run the client (compile first)
client: compile
	@(java $(JAVA_FLAGS) Client)

# Clean up compiled files
clean:
	rm -rf $(BIN)

