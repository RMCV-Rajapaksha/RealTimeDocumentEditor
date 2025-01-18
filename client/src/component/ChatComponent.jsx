import { useState, useEffect } from "react";
import { io } from "socket.io-client";
import { useParams } from "react-router-dom";

const socket = io("http://localhost:3001");

export default function ChatComponent() {
  const { id: documentId } = useParams();
  const [message, setMessage] = useState("");
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    console.log("Joining room:", documentId);
    socket.emit("join-room", documentId);

    socket.on("receive-message", (message) => {
      console.log("Received message:", message);
      setMessages((prevMessages) => [...prevMessages, message]);
    });

    return () => {
      socket.off("receive-message");
    };
  }, [documentId]);

  const sendMessage = () => {
    if (message.trim() === "") return; // Prevent sending empty messages
    console.log("Sending message:", message);
    socket.emit("send-message", message);
    setMessages((prevMessages) => [...prevMessages, message]);
    setMessage("");
  };

  return (
    <div className="p-4 mt-4 bg-gray-100 border border-gray-300 rounded-lg chat-container">
      <div className="mb-4 overflow-y-auto messages max-h-64">
        {messages.map((msg, index) => (
          <div key={index} className="p-2 border-b border-gray-300 message">
            {msg}
          </div>
        ))}
      </div>
      <div className="flex input-container">
        <input
          type="text"
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Type a message..."
          className="flex-1 p-2 border border-gray-300 rounded-l-lg input"
        />
        <button
          onClick={sendMessage}
          className="p-2 text-white bg-blue-500 rounded-r-lg send-button"
        >
          Send
        </button>
      </div>
    </div>
  );
}