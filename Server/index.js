const express = require("express");
const dotenv = require("dotenv");
const cors = require("cors");
dotenv.config();
const app = express();
const PORT = process.env.PORT || 4000;

app.use(express.json()); // Parse JSON bodies
app.use(cors()); // Enable CORS

// Basic route
app.get("/", (req, res) => {
  res.send("Hello, World!");
});

// Start server
app.listen(PORT, () => {
  console.log(`Server running on http://localhost:${PORT}`);
});

const io = require("socket.io")(3001, {
  cors: {
    origin: "http://localhost:3000",
    methods: ["GET", "POST", "PUT", "DELETE"],
  },
});

const defaultValue = "";

io.on("connection", (socket) => {
  console.log("Client connected successfully");

  socket.on("get-document", (documentId) => {
    socket.join(documentId);
    socket.emit("load-document", defaultValue);

    socket.on("send-changes", (delta) => {
      socket.broadcast.to(documentId).emit("receive-changes", delta);
    });

    // Handle chat messages
    socket.on("send-message", (message) => {
      socket.broadcast.to(documentId).emit("receive-message", message);
    });
  });
});
