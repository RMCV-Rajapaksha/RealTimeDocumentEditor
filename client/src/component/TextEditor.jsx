import React, { useRef, useEffect } from 'react';
import "quill/dist/quill.snow.css";
import Quill from "quill";

function TextEditor() {
    const wrapperRef = useRef(null);

    useEffect(() => {
        if (wrapperRef.current == null) return;

        wrapperRef.current.innerHTML = "";
        const editor = document.createElement("div");
        wrapperRef.current.appendChild(editor);
        new Quill(editor, {
            theme: "snow",
        });

    }, []);

    return (
        <div id="container" ref={wrapperRef}></div>
    );
}

export default TextEditor;