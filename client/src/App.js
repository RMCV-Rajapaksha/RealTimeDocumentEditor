import { Route, Routes } from "react-router-dom";
import PublicLayout from "./component/layout/PublicLayout";
import Document from "./pages/Document";

function App() {
  return (
    <>
      <Routes>
        <Route element={<PublicLayout />}>
          <Route path="document" element={<Document />} />
        </Route>
      </Routes>
    </>
  );
}

export default App;
