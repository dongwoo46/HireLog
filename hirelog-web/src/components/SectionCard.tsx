type SectionCardProps = {
  title: string;
  children: string;
};

export default function SectionCard({ title, children }: SectionCardProps) {
  return (
    <section className="rounded-lg border border-gray-200 bg-white p-5">
      <h2 className="mb-2 text-lg font-semibold text-gray-800">{title}</h2>
      <p className="whitespace-pre-line leading-relaxed text-gray-700">
        {children}
      </p>
    </section>
  );
}
