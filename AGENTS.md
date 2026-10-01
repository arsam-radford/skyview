# CS220 Project Standards

These rules apply to all work under this directory.

## Language and Submission

- Build Java applications, not applets. Other languages may be used only for required supporting components.
- Submit each project as one ZIP file named `hosseinimarashi-<ProjectName>.zip` (for example, `hosseinimarashi-Project1.zip`).
- Do not place unrelated projects in the same ZIP file.
- Every project must be demonstrated in lab by its announced deadline.

## Documentation and Attribution

- Follow the [Oracle Javadoc standard](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/javadoc.html) throughout every project.
- Use meaningful class, constant, variable, and method names so the code is self-documenting.
- Add comments where they explain intent or a non-obvious decision; do not narrate obvious code.
- Cite every external source used.
- Cite any AI assistance used during code development.
- Write only code the student can explain line by line.

## Design and Style

Every project must:

- Use named constants instead of unexplained literal constants.
- Keep each method under 30 non-blank lines.
- Give each method one responsibility. If its purpose requires “and” or “or” to describe, split it.
- Model each object as one coherent entity.
- Put only one functional statement on each line.
- Give every loop one entry and one exit.
- Never use `break` inside a loop; use `break` only in a `switch` statement.
- Provide a dedicated driver class containing only `main` and no other methods.
- Name the driver `<ProjectName>Driver.java` (for example, `Project1Driver.java`).
- Keep `main` out of classes that define additional methods.
- Avoid static methods other than `main`.
- Use inheritance only where the problem naturally defines an “is-a” relationship.
- Prefer short, descriptive methods and well-designed data over procedural or monolithic code.

Correct output alone is insufficient: grading also covers design, style, documentation, submission format, and the required lab demonstration.
