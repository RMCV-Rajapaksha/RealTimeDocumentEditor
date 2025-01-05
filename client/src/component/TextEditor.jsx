import React, { useRef, useEffect } from 'react';
import "quill/dist/quill.snow.css";
import Quill from "quill";
import "../styles.css";


const TOOLBAR_OPTIONS = [
    [{ header: [1, 2, 3, 4, 5, 6, false] }],
    [{ font: [] }],
    [{ list: "ordered" }, { list: "bullet" }],
    ["bold", "italic", "underline"],
    [{ color: [] }, { background: [] }],
    [{ script: "sub" }, { script: "super" }],
    [{ align: [] }],
    ["image", "blockquote", "code-block"],
    ["clean"],
    ["link"],
];

function TextEditor() {
    const wrapperRef = useRef(null);

    useEffect(() => {
        if (wrapperRef.current == null) return;

        wrapperRef.current.innerHTML = "";
        const editor = document.createElement("div");
        wrapperRef.current.appendChild(editor);
        new Quill(editor, {
            theme: "snow", modules: { toolbar: TOOLBAR_OPTIONS }
        });

    }, []);

    return (
        <div id="container" ref={wrapperRef}></div>
    );
}

export default TextEditor;